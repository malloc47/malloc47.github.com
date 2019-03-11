with import <nixpkgs> {};

stdenv.mkDerivation {
  name = "www";

  buildInputs = with pkgs; [
    (jekyll.override { withOptionalDependencies = true; })
  ];
}
