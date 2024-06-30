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

      systems = [
        x86_system
        arm_system
      ];
      eachSystem = pkgs.lib.genAttrs systems;

      getReleaseSystem = system: {
        "x86_64-linux" = "linux-x86_64";
        "aarch64-linux" = "linux-arm64";
      }.${system};

      kotlin_binary_patch_command = "patchkotlinbinary";

      build_shell_hook = ''
        # Add NIX_LDFLAGS to LD_LIBRARY_PATH
        lib_paths=($(echo $NIX_LDFLAGS | grep -oP '(?<=-rpath\s| -L)[^ ]+'))
        lib_paths_str=$(IFS=:; echo "''${lib_paths[*]}")
        export LD_LIBRARY_PATH="$lib_paths_str:$LD_LIBRARY_PATH"

        # Add include paths in NIX_CFLAGS_COMPILE to C_INCLUDE_PATH
        IFS=' ' read -r -a flags <<< "$NIX_CFLAGS_COMPILE"
        for (( i=0; i<''${#flags[@]}; i++ )); do
          if [ "''${flags[$i]}" == "-isystem" ] && [ $((i+1)) -lt ''${#flags[@]} ]; then
            export C_INCLUDE_PATH="''${C_INCLUDE_PATH}:''${flags[$((i+1))]}"
          fi
        done

        export KONAN_DATA_DIR=$(pwd)/.konan

        mkdir -p $KONAN_DATA_DIR
        cp -asfT ${custom_pkgs.kotlin-native-toolchain-env} $KONAN_DATA_DIR
        chmod -R u+w $KONAN_DATA_DIR

        mkdir $KONAN_DATA_DIR/bin
        export PATH="$KONAN_DATA_DIR/bin:$PATH"

        PATCH_KOTLIN_BINARY_SCRIPT="patchelf --set-interpreter \$(cat \$NIX_CC/nix-support/dynamic-linker) --set-rpath ${custom_pkgs.kotlin-native-toolchain-env}/dependencies/x86_64-unknown-linux-gnu-gcc-8.3.0-glibc-2.19-kernel-4.9-2/x86_64-unknown-linux-gnu/sysroot/lib64 \$1"
        echo "$PATCH_KOTLIN_BINARY_SCRIPT" > $KONAN_DATA_DIR/bin/${kotlin_binary_patch_command}
        chmod +x $KONAN_DATA_DIR/bin/${kotlin_binary_patch_command}

        chmod -R u+w $KONAN_DATA_DIR
      '';

      build_packages = with pkgs; [
        jdk21_headless
        jdk22
        pkg-config
        #cmake
        jextract
        mpv
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
        glibc
        glibc_multi
        libgcc.lib
        libayatana-appindicator
        libxcrypt-legacy.out
      ];
    in
    {
      packages = eachSystem (system: {
        default =
          pkgs.stdenv.mkDerivation {
            name = "spmp-server";
            src = ./.;

            nativeBuildInputs = with pkgs; [
              wget
              autoPatchelfHook
            ];
            buildInputs = runtime_packages;

            buildPhase = ''
              version=$(grep -m 1 '^project.version=' gradle.properties | sed 's/^project.version=//')
              if [[ -z "$version" ]]; then
                echo "project.version not found in gradle.properties"
                exit 1
              fi

              wget https://github.com/toasterofbread/spmp-server/releases/download/$version/spms-$version-${getReleaseSystem system}.kexe -O spms.kexe
            '';

            installPhase = ''
              mkdir -p $out/bin/spms
              install -Dm755 app/build/bin/linuxX64/debugExecutable/*.kexe $out/bin/spms
            '';

            outputHashAlgo = "sha256";
            outputHashMode = "recursive";
            outputHash = "sha256-Om4BcXK76QrExnKcDzw574l+h75C8yK/EbccpbcvLsQ=";
          };
      });

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
