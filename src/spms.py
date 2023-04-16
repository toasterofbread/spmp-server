import zmq
from os import path
import json
from player import Player

DEFAULT_PORT = 3973
DEFAULT_CONFIG_PATH = path.expanduser("~/.config/spmp.json")

CONFIG_KEY_PORT = "server_port"

class SpMs:

    def __init__(self, config_path: str):
        self.player = Player()

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

        self.socket = self.context.socket(zmq.REP)
        self.socket.bind(f"tcp://127.0.0.1:{port}")

    def poll(self):

        print("Polling started")
        while self.socket:

            msg: list[bytes] = self.socket.recv_multipart()

            action_name = msg.pop(0).decode()

            params = [json.loads(param.decode()) for param in msg]

            reply = None
            error: str | None = None

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

            if action_name != "get":
                print(f"{action_name}({params}) -> {reply} ({error})")

            encoded_reply: list[bytes] = [json.dumps(reply).encode()]
            if error is not None:
                encoded_reply.append(error.encode())

            self.socket.send_multipart(encoded_reply)

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
