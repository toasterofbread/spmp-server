# THIS DOES NOT WORK
# The libraries compile successfully, but mpv doesn't seem to actually statically link any of these dependencies to libmpv
# So when linking libmpv to spmp-server, any reference to a dependency is undefined
# Even when linking the dependencies directly to spmp-server, other issues occur (seemingly multiple-definition conflicts due to linking gcc multiple times)

# See https://github.com/mpv-player/mpv/issues/7738#issuecomment-1926868427

MPV_VERSION=0.38.0

set -e

if [ ! $# -eq 3 ]; then
    echo "Expected arguments: path to output directory, path to Kotlin/Native GCC toolchain, toolchain architecture"
    exit
fi

FINAL_OUTPUT_DIR=$(realpath $1)
TOOLCHAIN_DIR=$(realpath $2)
ARCH=$3

echo "mpv will be built to $FINAL_OUTPUT_DIR using $ARCH $TOOLCHAIN_DIR"

mkdir -p build/libmpv
cd build/libmpv

OUTPUT_DIR=$(pwd)/output

TOOLCHAIN_INSTALL_PREFIX=$TOOLCHAIN_DIR/$ARCH
TOOLCHAIN_BIN_PREFIX=$TOOLCHAIN_DIR/bin/$ARCH

export LT_SYS_LIBRARY_PATH=$LIBRARY_PATH
export CC=$TOOLCHAIN_BIN_PREFIX-cc
export CXX=$TOOLCHAIN_BIN_PREFIX-c++
export CMAKE_CXX_FLAGS="-fPIC"
export CMAKE_C_FLAGS="-fPIC"

if [ ! -d mpv ]; then
    echo "Downloading mpv..."

    wget https://github.com/mpv-player/mpv/archive/refs/tags/v$MPV_VERSION.tar.gz -O mpv.tar.gz
    tar -xzf mpv.tar.gz
    mv mpv-$MPV_VERSION mpv
    rm mpv.tar.gz
fi

if [ ! -d libplacebo ]; then
    echo "Downloading libplacebo..."

    git clone https://code.videolan.org/videolan/libplacebo.git --depth=1 --recursive
fi

if [ ! -d ffmpeg ]; then
    echo "Downloading ffmpeg..."

    git clone https://git.ffmpeg.org/ffmpeg.git --depth=1 --recursive
fi

if [ ! -d libass ]; then
    echo "Downloading libass..."

    wget https://github.com/libass/libass/releases/download/0.17.1/libass-0.17.1.tar.gz -O libass.tar.gz
    tar -xzf libass.tar.gz
    mv libass-0.17.1 libass
    rm libass.tar.gz
fi

if [ ! -d freetype ]; then
    echo "Downloading freetype..."

    wget https://downloads.sourceforge.net/freetype/freetype-2.13.2.tar.xz -O freetype.tar.gz
    tar -xf freetype.tar.gz
    mv freetype-2.13.2 freetype
    rm freetype.tar.gz
fi

if [ ! -d fribidi ]; then
    echo "Downloading fribidi..."

    wget https://github.com/fribidi/fribidi/releases/download/v1.0.14/fribidi-1.0.14.tar.xz -O fribidi.tar.gz
    tar -xf fribidi.tar.gz
    mv fribidi-1.0.14 fribidi
    rm fribidi.tar.gz
fi

if [ ! -d harfbuzz ]; then
    echo "Downloading harfbuzz..."

    wget https://github.com/harfbuzz/harfbuzz/releases/download/8.4.0/harfbuzz-8.4.0.tar.xz -O harfbuzz.tar.gz
    tar -xf harfbuzz.tar.gz
    mv harfbuzz-8.4.0 harfbuzz
    rm harfbuzz.tar.gz
fi

echo "Compiling libplacebo..."

cd libplacebo
meson setup build --reconfigure --clearcache --prefix=$OUTPUT_DIR -Dauto_features=disabled -Ddefault_library=static -Dc_link_args="-fPIC"
meson compile -C build
meson install -C build
cd ..

echo "Compiling freetype"

cd freetype
meson setup build \
    --prefix=$OUTPUT_DIR \
    --libdir=$OUTPUT_DIR/lib \
    --reconfigure \
    --clearcache \
    -Dauto_features=disabled \
    -Ddefault_library=static \
    -Dstrip=true \
    -Dc_link_args="-fPIC"
meson compile -C build
meson install -C build
cd ..

echo "Compiling fribidi"

cd fribidi
meson setup build \
    --prefix=$OUTPUT_DIR \
    --libdir=$OUTPUT_DIR/lib \
    --reconfigure \
    --clearcache \
    -Dauto_features=disabled \
    -Ddefault_library=static \
    -Dstrip=true \
    -Dc_link_args="-fPIC"
meson compile -C build
meson install -C build
cd ..

echo "Compiling harfbuzz"

cd harfbuzz

mkdir -p build
cd build

# Meson doesn't use the specified compiler here for some reason
cmake -GNinja \
    -DCMAKE_CXX_COMPILER=$CXX \
    -DCMAKE_C_COMPILER=$CC \
    -DCMAKE_INSTALL_PREFIX=$OUTPUT_DIR \
    -DCMAKE_CXX_FLAGS="-fPIC" \
    -DCMAKE_C_FLAGS="-fPIC" \
    ..
ninja install
cd ../..

echo "Compiling libass..."

cd libass
./configure \
    --prefix=$OUTPUT_DIR \
    --disable-libunibreak \
    --disable-require-system-font-provider \
    --disable-fontconfig \
    --disable-asm
make install -j$(nproc)
cd ..

echo "Compiling ffmpeg..."

cd ffmpeg
./configure \
    --prefix=$OUTPUT_DIR \
    --disable-doc \
    --disable-everything \
    --cc=$CC \
    --cxx=$CXX
make install -j$(nproc)
cd ..

echo "Compiling mpv..."

cd mpv
meson setup build \
    --prefix=$OUTPUT_DIR \
    --libdir=$OUTPUT_DIR/lib \
    --reconfigure \
    --clearcache \
    -Dbuildtype=release \
    -Dstrip=true \
    -Dprefer_static=true \
    -Ddefault_library=static \
    -Dlibmpv=true \
    -Dauto_features=disabled \
    -Dgl=disabled \
    -Dcplayer=false \
    -Dbuild-date=false \
    -Dlua=disabled \
    -Dc_link_args="-fPIC"

meson compile -C build
meson install -C build

echo "Copying files to output directory"
cp -r $OUTPUT_DIR/* $FINAL_OUTPUT_DIR

echo "Done"
