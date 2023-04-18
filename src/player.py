import mpv
from enum import Enum
from typing import Callable
import yt_dlp

ID = "LoK17z6xDwI"

def mpvLog(loglevel, component, message):
    print('[{}] {}: {}'.format(loglevel, component, message))

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

class Event:
    current_client_id: bytes | None = None

    def __init__(self, properties: list[str]):
        assert("id" not in properties and "type" not in properties)
        self.properties = properties

        self.id = -1

        assert(Event.current_client_id is not None)
        self.client_id = Event.current_client_id

    def init(self, id: int, client_amount: int):
        self.id = id
        self.client_amount = client_amount

    def toDict(self) -> dict:
        ret = {"type": type(self).__name__[:-5], "id": self.id}
        for key in self.properties:
            ret[key] = self.__getattribute__(key)
        return ret

class SongTransitionEvent(Event):
    def __init__(self, index: int):
        super().__init__(["index"])
        self.index = index

class PropertyChangedEvent(Event):
    def __init__(self, key: str, value):
        super().__init__(["key", "value"])
        self.key = key
        self.value = value

class SeekedEvent(Event):
    def __init__(self, position_ms: int):
        super().__init__(["position_ms"])
        self.position_ms = position_ms

class SongAddedEvent(Event):
    def __init__(self, song_id: str, index: int):
        super().__init__(["song_id", "index"])
        self.song_id = song_id
        self.index = index

class SongRemovedEvent(Event):
    def __init__(self, index: int):
        super().__init__(["index"])
        self.index = index

class SongMovedEvent(Event):
    def __init__(self, from_index: int, to_index: int):
        super().__init__(["from_index", "to_index"])
        self.from_index = from_index
        self.to_index = to_index

class Player:

    def __init__(
        self,
        id2filename: Callable = lambda f: f,
        filename2id: Callable = lambda id: id,
        eventListener: Callable | None = None
    ):
        self.id2filename = id2filename
        self.filename2id = filename2id
        self.eventListener = eventListener

        self.player = mpv.MPV(log_handler=mpvLog, ytdl=True, vid="no", start_event_thread=True)

        @self.player.property_observer("playlist")
        def onFilenameChanged(a, b):
            print(f"CHANGED {a} {b}")

    def _onEvent(self, event: Event):
        if self.eventListener is not None:
            self.eventListener(event)

    @property
    def state(self) -> PlayerState:
        # raise NotImplementedError()
        return PlayerState.IDLE

    @property
    def is_playing(self) -> bool:
        return not self.player.core_idle
        #return self.state == PlayerState.READY and not self.player.pause

    @property
    def song_count(self) -> int:
        return self.player.playlist_count # type: ignore

    @property
    def current_song_index(self) -> int:
        return self.player.playlist_playing_pos # type: ignore

    @property
    def current_position_ms(self) -> int:
        return int((self.player.playback_time or 0) * 1000) # type: ignore

    @property
    def duration_ms(self) -> int:
        return int((self.player.duration or 0) * 1000) # type: ignore

    @property
    def shuffle_enabled(self) -> bool:
        return False # TODO

    @property
    def repeat_mode(self) -> RepeatMode:
        return RepeatMode.OFF # TODO

    @property
    def volume(self) -> float:
        return self.player.volume # type: ignore

    @property
    def playlist(self) -> list:
        return [self.filename2id(filename) for filename in self.player.playlist_filenames]

    STATE_PROPERTIES = (is_playing, current_song_index, current_position_ms, duration_ms, shuffle_enabled, repeat_mode, volume, playlist)

    def getCurrentState(self) -> dict:
        return {prop.fget.__name__: prop.__get__(self) for prop in self.STATE_PROPERTIES}

    def play(self):
        if self.player.paused:
            self.player.paused = False
            self._onEvent(PropertyChangedEvent("is_playing", self.is_playing))

    def pause(self):
        if not self.player.paused:
            self.player.paused = True
            self._onEvent(PropertyChangedEvent("is_playing", self.is_playing))

    def playPause(self):
        self.player.paused = not self.player.paused
        self._onEvent(PropertyChangedEvent("is_playing", self.is_playing))

    def seekTo(self, position_ms: int):
        seconds, position_ms = divmod(position_ms, 1000)
        minutes, seconds = divmod(seconds, 60)
        hours, minutes = divmod(minutes, 60)

        seek_time =  f"{hours:02d}:{minutes:02d}:{seconds:02d}.{position_ms//100}"
        self.player.seek(seek_time, "absolute", "exact")

        self._onEvent(SeekedEvent(position_ms))

    def seekToIndex(self, index: int, position_ms: int):
        if index == self.current_song_index or index < 0 or index >= self.song_count:
            return

        self.player.playlist_play_index(index)
        self._onEvent(SongTransitionEvent(index))

        if position_ms > 0:
            self.seekTo(position_ms)

    def seekToNext(self):
        index = self.current_song_index
        self.player.playlist_next()

        new_index = self.current_song_index
        if index != new_index:
            self._onEvent(SongTransitionEvent(index))

    def seekToPrevious(self):
        index = self.current_song_index
        self.player.playlist_prev()

        new_index = self.current_song_index
        if index != new_index:
            self._onEvent(SongTransitionEvent(index))

    def getSong(self, index: int = -1) -> str | None:
        if not self.hasSong(index):
            return None

        if index < 0:
            index = self.current_song_index

        return self.filename2id(self.player.playlist_filenames[index])

    def addSong(self, id: str, index: int = -1):
        url = self.id2filename(id)

        if self.song_count == 0:
            self.player.loadfile(url)
        else:
            self.player.playlist_append(url)

        if index >= self.song_count or index < 0:
            index = self.song_count - 1
        elif index + 1 < self.song_count:
            self.moveSong(self.song_count - 1, index, False)

        self._onEvent(SongAddedEvent(id, index))

    def moveSong(self, from_index: int, to_index: int, add_event: bool = True):
        if from_index == to_index or not self.hasSong(from_index) or not self.hasSong(to_index):
            return

        self.player.playlist_move(from_index, to_index)

        if add_event:
            self._onEvent(SongMovedEvent(from_index, to_index))

    def removeSong(self, index: int):
        if not self.hasSong(index):
            return

        self.player.playlist_remove(str(index))

        self._onEvent(SongRemovedEvent(index))

    def setRepeatMode(self, value: RepeatMode):
        if value == self.repeat_mode:
            return
        # TODO
        self._onEvent(PropertyChangedEvent("repeat_mode", self.repeat_mode))

    def setVolume(self, value: float):
        if value == self.volume:
            return
        self.player.volume = value
        self._onEvent(PropertyChangedEvent("volume", self.volume))

    ACTIONS = (play, pause, playPause, seekTo, seekToIndex, seekToNext, seekToPrevious, getSong, addSong, moveSong, removeSong, setRepeatMode, setVolume)

    def hasSong(self, index: int) -> bool:
        return index >= 0 and index + 1 < self.song_count

    def getStreamUrl(self, video_id: str) -> str:
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
