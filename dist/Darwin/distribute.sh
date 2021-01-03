#!/bin/bash

set -euax

SRC_IMAGE=$BASE/src/com/gcodebuilder/app/images/gcode_builder_icon_1024x1024.png
ICNS_NAME=gcodebuilder_app
ICONSET_PATH=$PLATFORM_DIR/out/${ICNS_NAME}.iconset
APP_PATH=$PLATFORM_DIR/out/GCodeBuilder.app

if [ -e "$ICONSET_PATH" ]; then
    /bin/rm -rf "$ICONSET_PATH"
fi

/bin/mkdir -p "$ICONSET_PATH"

icon_file_list=(
    "icon_16x16.png"
    "icon_16x16@2x.png"
    "icon_32x32.png"
    "icon_32x32@2x.png"
    "icon_128x128.png"
    "icon_128x128@2x.png"
    "icon_256x256.png"
    "icon_256x256@2x.png"
    "icon_512x512.png"
    "icon_512x512@2x.png"
    )

icon_size=(
    '16'
    '32'
    '32'
    '64'
    '128'
    '256'
    '256'
    '512'
    '512'
    '1024'
    )

counter=0
for a in ${icon_file_list[@]}; do
    icon="${ICONSET_PATH}/${a}"
    /bin/cp "$SRC_IMAGE" "$icon"
    icon_size=${icon_size[$counter]}
    /usr/bin/sips -z $icon_size $icon_size "$icon"
    counter=$(($counter + 1))
done

/usr/bin/iconutil -c icns "$ICONSET_PATH"

if [ -e "$APP_PATH" ]; then
    /bin/rm -rf "$APP_PATH"
fi

$PACKR $PLATFORM_DIR/packr-config.json
