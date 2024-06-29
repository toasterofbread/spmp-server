{
  description = "SpMp server development environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    custom_nixpkgs.url = "github:toasterofbread/nixpkgs/4df73973bda897522847e03e0820067c053bccad";
  };

  outputs = { self, nixpkgs, custom_nixpkgs, ... }:
    let
      x86_system = "x86_64-linux";
      arm_system = "aarch64-linux";

      pkgs = import nixpkgs {
        system = x86_system;
      };
      custom_pkgs = import custom_nixpkgs {
        system = x86_system;
      };

      kotlin_binary_patch_command = "patchkotlinbinary";

      build_shell_hook = ''
      echo 1
        # Add NIX_LDFLAGS to LD_LIBRARY_PATH
        lib_paths=($(echo $NIX_LDFLAGS | grep -oP '(?<=-rpath\s| -L)[^ ]+'))
      echo 2
        lib_paths_str=$(IFS=:; echo "''${lib_paths[*]}")
      echo 3
        export LD_LIBRARY_PATH="$lib_paths_str:$LD_LIBRARY_PATH"
      echo 4

        # Add glibc and glibc_multi to C_INCLUDE_PATH
        export C_INCLUDE_PATH="${pkgs.glibc.dev}/include:${pkgs.glibc_multi.dev}/include:$C_INCLUDE_PATH"
      echo 5

        export KONAN_DATA_DIR=$(pwd)/.konan

      echo 6
        mkdir -p $KONAN_DATA_DIR
      echo 7
        cp -asfT ${custom_pkgs.kotlin-native-toolchain-env} $KONAN_DATA_DIR
      echo 8
        chmod -R u+w $KONAN_DATA_DIR
      echo 9

        mkdir $KONAN_DATA_DIR/bin
      echo 10
        export PATH="$KONAN_DATA_DIR/bin:$PATH"
      echo 11

        PATCH_KOTLIN_BINARY_SCRIPT="patchelf --set-interpreter \$(cat \$NIX_CC/nix-support/dynamic-linker) --set-rpath $KONAN_DATA_DIR/dependencies/x86_64-unknown-linux-gnu-gcc-8.3.0-glibc-2.19-kernel-4.9-2/x86_64-unknown-linux-gnu/sysroot/lib64 \$1"
      echo 12
        echo "$PATCH_KOTLIN_BINARY_SCRIPT" > $KONAN_DATA_DIR/bin/${kotlin_binary_patch_command}
      echo 13
        chmod +x $KONAN_DATA_DIR/bin/${kotlin_binary_patch_command}
      echo 14

        chmod -R u+w $KONAN_DATA_DIR
      echo 15
      '';

      build_packages = with pkgs; [
        jdk21_headless
        jdk22
        pkg-config
        #cmake
        jextract
        mpv
        libayatana-appindicator
        gtk3
        curl
        patchelf
        glibc
        glibc_multi
        libgcc.lib
        (custom_pkgs.zeromq-kotlin-native.override { enableDrafts = true; })
        (custom_pkgs.kotlin-native-toolchain-env.override { x86_64 = true; aarch64 = true; })
      ];

      runtime_packages = with pkgs; [
        jdk22
        patchelf
        glibc
        glibc_multi
        libgcc.lib
      ];
    in
    {
      packages."${x86_system}".default =
        pkgs.stdenv.mkDerivation {
          name = "spmp-server";
          src = ./.;

          nativeBuildInputs = with pkgs; [
            gradle
          ] ++ build_packages;
          buildInputs = runtime_packages;


          buildPhase = ''
          echo aaaaaaaa
            ${build_shell_hook}
          echo bbbbbbbbb

            export JAVA_21_HOME="${pkgs.jdk21_headless}/lib/openjdk";
            export JAVA_22_HOME="${pkgs.jdk22}/lib/openjdk";
            export JAVA_HOME="${pkgs.jdk21_headless}/lib/openjdk";
            export JEXTRACT_PATH="${pkgs.jextract}/bin/jextract";
            export KOTLIN_BINARY_PATCH_COMMAND="${kotlin_binary_patch_command}";

            gradle app:linuxX64Binaries
          '';

          installPhase = ''
            mkdir -p $out/bin/spms
            install -Dm755 app/build/bin/linuxX64/debugExecutable/*.kexe $out/bin/spms
          '';

          outputHashAlgo = "sha256";
          outputHashMode = "recursive";
          outputHash = "sha256-Om4BcXK76QrExnKcDzw574l+h75C8yK/EbccpbcvLsQ=";
        };

      devShells."${x86_system}".default =
        let
          pkgs = import nixpkgs {
            system = x86_system;
          };
          arm_pkgs = import nixpkgs {
            system = arm_system;
          };
          custom_pkgs = import custom_nixpkgs {
            system = x86_system;
          };
        in
        pkgs.mkShell {
          packages = build_packages ++ runtime_packages;

          JAVA_21_HOME = "${pkgs.jdk21_headless}/lib/openjdk";
          JAVA_22_HOME = "${pkgs.jdk22}/lib/openjdk";
          JAVA_HOME = "${pkgs.jdk21_headless}/lib/openjdk";
          JEXTRACT_PATH = "${pkgs.jextract}/bin/jextract";

          KOTLIN_BINARY_PATCH_COMMAND = kotlin_binary_patch_command;

          shellHook = build_shell_hook;
        };
    };
}
