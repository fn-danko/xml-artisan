{
  description = "XML Artisan - Development Environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        jdk = pkgs.temurin-bin-21;

      in {
        devShells.default = pkgs.mkShell {
          name = "xml-artisan-shell";

          buildInputs = [
            jdk
            pkgs.jdt-language-server
            pkgs.git
          ];

          shellHook = ''
            export JAVA_HOME="${jdk}"
            export PATH="$JAVA_HOME/bin:$PATH"
            export ENABLE_LSP_TOOL=1

            echo "🚀 XML Artisan Development Shell"
            echo "Java version: $(java -version 2>&1 | head -1)"
          '';
        };
      });
}
