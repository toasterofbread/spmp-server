import zmq
from os import path
import json
from player import Player, Event
import time

DEFAULT_PORT = 3973
DEFAULT_CONFIG_PATH = path.expanduser("~/.config/spmp.json")

CONFIG_KEY_PORT = "server_port"

class Client:
    def __init__(self, id: bytes, name: str | None, event_head: int):
        self.id = id
        self.name = name
        self.event_head = event_head

def getMsgClient(msg: list[bytes]) -> bytes:
    return msg.pop(0)

class SpMs:

    def __init__(self, config_path: str):
        self.clients: dict[bytes, Client] = {}
        self.events: list[Event] = []
        self.event_count = 0

        def onEvent(event: Event):
            event.init(self.event_count, len(self.events))
            self.event_count += 1
            self.events.append(event)
        self.player = Player(onEvent)

        self.context = zmq.Context()
        self.socket = None

        self.config_path = config_path
        self.loadConfig()

    def loadConfig(self):
        if path.exists(self.config_path):
            f = open(self.config_path, "r")
            self.config: dict = json.loads(f.read())
            f.close()
        else:
            self.config = {CONFIG_KEY_PORT: DEFAULT_PORT}

    def bind(self, port: int | None):
        if self.socket is not None:
            raise RuntimeError("Already bound")

        if port is None:
            port = self.config[CONFIG_KEY_PORT]
            if port is None:
                raise RuntimeError("No port specified")

        self.socket = self.context.socket(zmq.ROUTER)
        self.socket.bind(f"tcp://127.0.0.1:{port}")

        self.poller = zmq.Poller()
        self.poller.register(self.socket, zmq.POLLIN)

    def onClientConnected(self, client_id: bytes, msg: list[bytes]):
        assert(client_id not in self.clients.keys())

        client_name = msg.pop(0).decode()
        self.clients[client_id] = Client(client_id, client_name, self.event_count)
        print(f"Client {client_name} connected")

    def onClientDisconnected(self, client_id: bytes):
        client_name = self.clients.pop(client_id).name
        print(f"Client {client_name} disconnected")

    def getEventsForClient(self, client_id: bytes) -> list[Event]:
        client = self.clients[client_id]
        ret = []

        for event in self.events:
            if event.id < client.event_head or event.client_id == client_id:
                continue

            ret.append(event)

            if event.client_amount == 1:
                self.events.remove(event)
            else:
                event.client_amount -= 1

        return ret

    def poll(self):

        print("Polling started")

        while self.socket:

            # Process stray messages (connecting clients)
            while True:
                try:
                    msg = self.socket.recv_multipart(zmq.NOBLOCK)
                    client = getMsgClient(msg)

                    if client not in self.clients:
                        self.onClientConnected(client, msg)
                except zmq.error.Again:
                    break

            # Send relevant events to each client
            for client in list(self.clients.values()):

                # Get events and send to client
                message: list[bytes] = [client.id]

                events = self.getEventsForClient(client.id)
                if len(events) == 0:
                    message.append(b"")
                else:
                    for event in events:
                        message.append(json.dumps(event.toDict()).encode())

                self.socket.send_multipart(message)

                print(f"Sent events {events} to client {client.name}")

                # Wait for client to reply...
                client_reply: list[bytes] | None = None
                for _ in self.poller.poll(1000):
                    reply = self.socket.recv_multipart()
                    reply_client = getMsgClient(reply)

                    if reply_client == client.id:
                        client_reply = reply
                    else: # Handle client connection during wait
                        self.onClientConnected(reply_client, reply)

                # Client did not reply to message within timeout
                if client_reply is None:
                    self.onClientDisconnected(client.id)
                    continue

                # Empty response
                if len(client_reply) == 1:
                    continue

                assert(len(client_reply) % 2 == 0)

                assert(Event.current_client_id is None)
                Event.current_client_id = client.id

                i = 0
                while (i < len(client_reply)):
                    action_name = client_reply[i].decode()
                    i += 1

                    params: list = json.loads(client_reply[i].decode())
                    assert(isinstance(params, list))
                    i += 1

                    print(f"Performing action {action_name}{params}")

                    found = False
                    for actions, instance in ((self.ACTIONS, self), (self.player.ACTIONS, self.player)):
                        for action in actions:
                            if action.__name__ != action_name and action.__name__.removeprefix("action").lower() != action_name:
                                continue

                            try:
                                reply = action(instance, *params) # type: ignore
                            except TypeError as e:
                                error = str(e)

                            found = True
                            break

                        if found:
                            break

                    if not found:
                        error = f"No action with name {action_name}"

                Event.current_client_id = None

            time.sleep(0.1)

    def actionGet(self, key: str):
        return self.player.getProperty(key)

    def actionSet(self, key: str, value):
        self.player.setProperty(key, value)
        return True

    ACTIONS = (actionGet, actionSet)

def main():
    from argparse import ArgumentParser

    parser = ArgumentParser("SpMs")
    parser.add_argument("-p", "--port")
    parser.add_argument("-c", "--config-path", default = DEFAULT_CONFIG_PATH)

    args = parser.parse_args()

    server = SpMs(args.config_path)
    server.bind(args.port)

    server.poll()

if __name__ == "__main__":
    main()
