from enum import Enum
import mpv
import yt_dlp
import requests

ID = "LoK17z6xDwI"

def mpvLog(loglevel, component, message):
    print('[{}] {}: {}'.format(loglevel, component, message))

# https://mpv.io/manual/master/#list-of-input-commands
# https://mpv.io/manual/master/#property-list
# player.pause
# player.duration (seconds)
# player.seek("00:02:14.0", "absolute", "exact")
# player.percent_pos
# player.time_pos (seconds)
# player.playlist_pos
# player.playlist_playing_pos (current pos)
# player.playlist_count
# player.playlist

class AudioQuality(int, Enum):
    LOW = 0
    MEDIUM = 1
    HIGH = 2

class RepeatMode(int, Enum):
    OFF = 0
    ONE = 1
    ALL = 2

class PlayerState(int, Enum):
    IDLE = 0
    BUFFERING = 1
    READY = 2
    ENDED = 3

class YoutubeStream:
    PROTOCOL = "yt"

    @staticmethod
    def url2id(url: str) -> str:
        return url[len(YoutubeStream.PROTOCOL) + 3:]
    @staticmethod
    def id2url(id: str) -> str:
        return f"{YoutubeStream.PROTOCOL}://{id}"

    def __init__(self, url: str):
        self.stream_url = getStreamUrl(YoutubeStream.url2id(url))
        self.response = requests.get(self.stream_url, stream=True)
        self.bytes_read = 0
        self.buffer = b''

    @property
    def size(self):
        content_length = self.response.headers.get("content-length")
        if content_length is not None:
            return int(content_length)
        else:
            return None

    def read(self, size):
        if len(self.buffer) < size:
            chunk = self.response.raw.read(size - len(self.buffer))
            if not chunk:
                return b''  # end of stream
            self.buffer += chunk
        data, self.buffer = self.buffer[:size], self.buffer[size:]
        self.bytes_read += len(data)
        return data

    def seek(self, pos):
        if pos < self.bytes_read:
            self.response.close()
            self.response = requests.get(self.stream_url, stream=True, headers={'Range': f'bytes={pos}-'})
            self.buffer = b''
            self.bytes_read = pos
        elif pos > self.bytes_read:
            delta = pos - self.bytes_read
            self.read(delta)
        return self.bytes_read

    def close(self):
        self.response.close()

    def cancel(self):
        self.response.close()

class PlayerEvent:
    def __init__(self):
        self.id = -1

    def setId(self, id: int):
        self.id = id

    def toDict(self) -> dict:
        return {"type": type(self).__name__[:-5], "id": self.id}

class SongTransitionEvent(PlayerEvent):
    def __init__(self, index: int):
        super().__init__()
        self.index = index

    def toDict(self) -> dict:
        ret = super().toDict()
        ret["index"] = self.index
        return ret

class PropertyChangedEvent(PlayerEvent):
    def __init__(self, property_key: str):
        super().__init__()
        self.property_key = property_key

    def toDict(self) -> dict:
        ret = super().toDict()
        ret["property_key"] = self.property_key
        return ret

class SongAddedEvent(PlayerEvent):
    def __init__(self, song_id: str, index: int):
        super().__init__()
        self.song_id = song_id
        self.index = index

    def toDict(self) -> dict:
        ret = super().toDict()
        ret["song_id"] = self.song_id
        ret["index"] = self.index
        return ret

class SongRemovedEvent(PlayerEvent):
    def __init__(self, index: int):
        super().__init__()
        self.index = index

    def toDict(self) -> dict:
        ret = super().toDict()
        ret["index"] = self.index
        return ret

class SongMovedEvent(PlayerEvent):
    def __init__(self, from_index: int, to_index: int):
        super().__init__()
        self.from_index = from_index
        self.to_index = to_index

    def toDict(self) -> dict:
        ret = super().toDict()
        ret["from_index"] = self.from_index
        ret["to_index"] = self.to_index
        return ret

class Player:

    events: list[PlayerEvent] = []

    def __init__(self):
        self.player = mpv.MPV(log_handler=mpvLog, ytdl=True, vid="no", start_event_thread=True)
        self.player.register_stream_protocol(YoutubeStream.PROTOCOL, lambda url: YoutubeStream(url))
        self.event_count = 0

    def _addEvent(self, event: PlayerEvent):
        assert(event.id == -1)

        event.setId(self.event_count)
        self.event_count += 1

        self.events.append(event)

    @property
    def is_playing(self) -> bool:
        return not self.player.pause # type: ignore

    @property
    def song_count(self) -> int:
        return self.player.playlist_count # type: ignore

    @property
    def current_song_index(self) -> int:
        return self.player.playlist_playing_pos # type: ignore

    @property
    def current_position_ms(self) -> int:
        return int((self.player.time_pos or 0) * 1000) # type: ignore

    @property
    def duration_ms(self) -> int:
        return int((self.player.duration or 0) * 1000) # type: ignore

    @property
    def shuffle_enabled(self) -> bool:
        return False # TODO

    @property
    def repeat_mode(self) -> RepeatMode:
        return RepeatMode.OFF # TODO

    @repeat_mode.setter
    def setRepeatMode(self, value: RepeatMode):
        # TODO
        self._addEvent(PropertyChangedEvent("repeat_mode"))

    @property
    def volume(self) -> float:
        return self.player.volume # type: ignore

    @volume.setter
    def setVolume(self, value: float):
        self.player.volume = value
        self._addEvent(PropertyChangedEvent("volume"))

    @property
    def playlist(self) -> list:
        return [YoutubeStream.url2id(url) for url in self.player.playlist_filenames]

    PROPERTIES = (is_playing, song_count, current_song_index, current_position_ms, duration_ms, shuffle_enabled, repeat_mode, volume, playlist)

    def getProperty(self, key: str):
        for prop in self.PROPERTIES:
            if prop.fget.__name__ == key:
                return prop.__get__(self)
        raise KeyError(key)

    def setProperty(self, key: str, value):
        for prop in self.PROPERTIES:
            if prop.fget.__name__ == key and prop.setter != None:
                self.__setattr__(key, value)
                return
        raise KeyError(key)

    def getEvents(self, from_id: int) -> dict:
        events = [event.toDict() for event in self.events if event.id > from_id]
        return {
            "events": events,
            "playlist": None if len(events) == 0 else self.playlist
        }

    def play(self):
        pass

    def pause(self):
        pass

    def playPause(self):
        pass

    def seekTo(self, position_ms: int):
        seconds, position_ms = divmod(position_ms, 1000)
        minutes, seconds = divmod(seconds, 60)
        hours, minutes = divmod(minutes, 60)

        seek_time =  f"{hours:02d}:{minutes:02d}:{seconds:02d}.{position_ms//100}"
        self.player.seek(seek_time, "absolute", "exact")

    def seekToIndex(self, index: int, position_ms: int):
        if index == self.current_song_index or index < 0 or index >= self.song_count:
            return

        self.player.playlist_play_index(index)

        if position_ms > 0:
            self.seekTo(position_ms)

        self._addEvent(SongTransitionEvent(index)) # type: ignore

    def seekToNext(self):
        index = self.current_song_index
        self.player.playlist_next()

        new_index = self.current_song_index
        if index != new_index:
            self._addEvent(SongTransitionEvent(index)) # type: ignore

    def seekToPrevious(self):
        index = self.current_song_index
        self.player.playlist_prev()

        new_index = self.current_song_index
        if index != new_index:
            self._addEvent(SongTransitionEvent(index)) # type: ignore

    def getSong(self, index: int = -1) -> str | None:
        if not self.hasSong(index):
            return None

        if index < 0:
            index = self.current_song_index

        url: str = self.player.playlist_filenames[index]
        return YoutubeStream.url2id(url)

    def addSong(self, id: str, index: int = -1):

        if self.song_count == 0:
            self.player.loadfile(YoutubeStream.id2url(id))
        else:
            self.player.playlist_append(YoutubeStream.id2url(id))

        if index >= self.song_count or index < 0:
            index = self.song_count - 1
        elif index + 1 < self.song_count:
            self.moveSong(self.song_count - 1, index)

        self._addEvent(SongAddedEvent(id, index))

    def moveSong(self, from_index: int, to_index: int):
        if from_index == to_index or not self.hasSong(from_index) or not self.hasSong(to_index):
            return

        self.player.playlist_move(from_index, to_index)

        self._addEvent(SongMovedEvent(from_index, to_index))

    def removeSong(self, index: int):
        if not self.hasSong(index):
            return

        self.player.playlist_remove(str(index))

        self._addEvent(SongRemovedEvent(index))

    ACTIONS = (getProperty, setProperty, getEvents, play, pause, playPause, seekTo, seekToIndex, seekToNext, seekToPrevious, getSong, addSong, moveSong, removeSong)

    def hasSong(self, index: int) -> bool:
        return index >= 0 and index + 1 < self.song_count

def getStreamUrl(video_id: str) -> str:
    ytd = yt_dlp.YoutubeDL({"skip_download": True, "quiet": True})

    info = ytd.extract_info(video_id, download=False)
    if info == None:
        raise RuntimeError("No formats returned by yt-dlp")

    audio_formats = []
    for format in info["formats"]:
        if format["video_ext"] != "none":
            continue
        if format["audio_ext"] == "none":
            continue
        audio_formats.append(format)

    return getFormatByQuality(audio_formats, AudioQuality.MEDIUM)["url"]

def getFormatByQuality(formats: list[dict], quality: AudioQuality) -> dict:
    formats.sort(key = lambda f: f["quality"], reverse = True)

    if quality == AudioQuality.HIGH:
        return formats[0]
    elif quality == AudioQuality.MEDIUM:
        return formats[len(formats)//2]
    elif quality == AudioQuality.LOW:
        return formats[-1]
    else:
        raise NotImplementedError(str(quality))
