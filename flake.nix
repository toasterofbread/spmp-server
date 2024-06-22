{
  description = "SpMp server development environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    custom_nixpkgs.url = "github:toasterofbread/nixpkgs/2fce494a8413016b5630cb593ffb9a0a1867274a";
  };

  outputs = { self, nixpkgs, custom_nixpkgs, ... }:
    let
      system = "x86_64-linux";
    in
    {
      devShells."${system}".default =
        let
          pkgs = import nixpkgs {
            inherit system;
          };
          custom_pkgs = import custom_nixpkgs {
            inherit system;
          };
        in
        pkgs.mkShell {
          packages = with pkgs; [
            fish
            jdk21
            jdk22
            pkg-config
            cmake
            jextract
            (custom_pkgs.zeromq-kotlin-native.override { enableDrafts = true; })
            mpv
            libayatana-appindicator
            gtk3
            curl
            (custom_pkgs.kotlin-native-toolchain-env.override { x86 = true; })
            git

            # Runtime
            patchelf
            glibc
            libgcc.lib
          ];

          JAVA_21_HOME = "${pkgs.jdk21_headless}/lib/openjdk";
          JAVA_22_HOME = "${pkgs.jdk22}/lib/openjdk";
          JAVA_HOME = "${pkgs.jdk21_headless}/lib/openjdk";
          JEXTRACT_PATH = "${pkgs.jextract}/bin/jextract";

          KOTLIN_BINARY_PATCH_COMMAND = "patchkotlinbinary";

          shellHook = ''
            # Add NIX_LDFLAGS to LD_LIBRARY_PATH
            lib_paths=($(echo $NIX_LDFLAGS | grep -oP '(?<=-rpath\s| -L)[^ ]+'))
            lib_paths_str=$(IFS=:; echo "''${lib_paths[*]}")
            export LD_LIBRARY_PATH="$lib_paths_str:$LD_LIBRARY_PATH"

            # Add glibc/include to C_INCLUDE_PATH
            export C_INCLUDE_PATH="${pkgs.glibc.dev}/include:$C_INCLUDE_PATH"

            echo "Using KJna development environment"
            echo "JAVA_HOME=$JAVA_HOME"

            export KONAN_DATA_DIR=$(pwd)/.konan

            mkdir -p $KONAN_DATA_DIR
            cp -asfT ${custom_pkgs.kotlin-native-toolchain-env} $KONAN_DATA_DIR

            mkdir $KONAN_DATA_DIR/bin
            export PATH="$KONAN_DATA_DIR/bin:$PATH"

            PATCH_KOTLIN_BINARY_SCRIPT="patchelf --set-interpreter \$(cat \$NIX_CC/nix-support/dynamic-linker) --set-rpath $KONAN_DATA_DIR/dependencies/x86_64-unknown-linux-gnu-gcc-8.3.0-glibc-2.19-kernel-4.9-2/x86_64-unknown-linux-gnu/sysroot/lib64 \$1"
            echo "$PATCH_KOTLIN_BINARY_SCRIPT" > $KONAN_DATA_DIR/bin/$KOTLIN_BINARY_PATCH_COMMAND
            chmod +x $KONAN_DATA_DIR/bin/$KOTLIN_BINARY_PATCH_COMMAND

            chmod -R u+w $KONAN_DATA_DIR
          '';
        };
    };
}
