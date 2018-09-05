#!/bin/bash

#  Usage on Linux:
#
#   $ curl https://codeload.github.com/opencv/opencv/zip/3.4.3 -o opencv-3.4.3.zip
#   $ unzip opencv-3.4.3.zip
#   $
#   $ export ANDROID_NDK=<absolute path to the android-ndk>
#   $ export ANDROID_HOME=<absolute path to Android SDK v25.2.5
#   $ ./build_opencv.sh
#
#   opencv will be installed into the `result`

set -ex

SRC_DIR="$PWD/opencv-3.4.3"
INSTALL_DIR="$PWD/result"

NCPU=$(nproc --all || sysctl -n hw.ncpu || echo "$NUMBER_OF_PROCESSORS" || 8)

my_cmake () {
cmake \
    -DANDROID_NATIVE_API_LEVEL=android-16 \
    -DANDROID_STL=c++_shared \
    -DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK}/build/cmake/android.toolchain.cmake \
    -DANDROID_TOOLCHAIN=clang \
    -DANDROID_CPP_FEATURES="rtti exceptions" \
    -DCMAKE_CXX_FLAGS_RELEASE="-ffunction-sections -fdata-sections -fstrict-aliasing -g0 -O3" \
    -DCMAKE_C_FLAGS_RELEASE="-ffunction-sections -fdata-sections -fstrict-aliasing -g0 -O3" \
    -DCMAKE_SHARED_LINKER_FLAGS_RELEASE="-Wl,--gc-sections" \
    -DCMAKE_BUILD_WITH_INSTALL_RPATH=ON \
    -DENABLE_THIN_LTO=ON \
    -DENABLE_PRECOMPILED_HEADERS=OFF \
    -DBUILD_ANDROID_EXAMPLES=OFF \
    -DBUILD_ANDROID_PROJECTS=OFF \
    -DBUILD_DOCS=OFF \
    -DBUILD_FAT_JAVA_LIB=OFF \
    -DBUILD_JASPER=OFF \
    -DBUILD_JAVA=OFF \
    -DBUILD_JPEG=OFF \
    -DBUILD_opencv_androidcamera=OFF \
    -DBUILD_opencv_apps=OFF \
    -DBUILD_opencv_calib3d=OFF \
    -DBUILD_opencv_contrib=OFF \
    -DBUILD_opencv_dnn=OFF \
    -DBUILD_opencv_features2d=OFF\
    -DBUILD_opencv_flann=OFF \
    -DBUILD_opencv_highgui=OFF \
    -DBUILD_opencv_imgcodecs=ON \
    -DBUILD_opencv_java=OFF \
    -DBUILD_opencv_legacy=OFF \
    -DBUILD_opencv_ml=ON \
    -DBUILD_opencv_nonfree=OFF \
    -DBUILD_opencv_objdetect=ON \
    -DBUILD_opencv_photo=OFF \
    -DBUILD_opencv_python_bindings_g=OFF \
    -DBUILD_opencv_stitching=OFF \
    -DBUILD_opencv_ts=OFF \
    -DBUILD_opencv_video=OFF \
    -DBUILD_opencv_videostab=OFF \
    -DBUILD_OPENEXR=OFF \
    -DBUILD_PACKAGE=OFF \
    -DBUILD_PERF_TESTS=OFF \
    -DBUILD_PNG=OFF \
    -DBUILD_PROTOBUF=OFF \
    -DBUILD_TESTS=OFF \
    -DBUILD_TIFF=OFF \
    -DBUILD_WEBP=OFF \
    -DBUILD_WITH_DEBUG_INFO=OFF \
    -DBUILD_ZLIB=OFF \
    -DCV_TRACE=OFF \
    -DWITH_CAROTENE=OFF \
    -DWITH_CPUFEATURES=ON \
    -DWITH_EIGEN=OFF \
    -DWITH_IMGCODEC_HDR=OFF \
    -DWITH_IMGCODEC_PXM=OFF \
    -DWITH_IMGCODEC_SUNRASTER=OFF \
    -DWITH_IPP=OFF \
    -DWITH_ITT=OFF \
    -DWITH_JASPER=OFF \
    -DWITH_JPEG=OFF \
    -DWITH_NVCUVID=OFF \
    -DWITH_OPENEXR=OFF \
    -DWITH_PNG=OFF \
    -DWITH_PROTOBUF=OFF \
    -DWITH_TBB=ON \
    -DWITH_TIFF=OFF \
    -DWITH_WEBP=OFF \
   "$@"
}

build_abi () {
    LOC_ABI=$1
    BUILD_SUBDIR=$2
    shift 2

    pushd .
    rm -rf "build/$BUILD_SUBDIR"
    mkdir -p "build/$BUILD_SUBDIR"
    cd "build/$BUILD_SUBDIR"
    my_cmake -DANDROID_ABI="$LOC_ABI" -DCMAKE_INSTALL_PREFIX=$INSTALL_DIR "$@" "$SRC_DIR"
    make -j$NCPU
    make install
    popd
}


mkdir -p $INSTALL_DIR
build_abi "armeabi-v7a with NEON" "armeabi_v7a_neon" "$@"
build_abi "x86" "x86" $@
build_abi "arm64-v8a" "arm64-v8a" $@
build_abi "x86_64" "x86_64" $@

