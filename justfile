set windows-shell := ["powershell.exe", "-NoLogo", "-Command"]

@_list:
    just --list --unsorted

alias verify-ci := verify-headless

# Builds the entire project - also downloading the harness if needed
build:
    ant build

# Creates the zipped application code in dist/trgtd.zip
build-zip:
    ant build-zip

# Verifies modules with headless tests
verify-headless *args:
    ./gradlew -p gradle check {{args}}

# Verifies all modules - including UI tests
verify *args:
    ./gradlew -p gradle check -Dtest.profile=allTests {{args}}

# Shows some diagnostics used by ant
diagnostics:
    ant -diagnostics

# Clears the cache of the local development version of ThinkingRock
[linux]
[macos]
clear-cache-dev:
	rm -rI ./build/testuserdir/var/cache/

# Clears the cache of the local development version of ThinkingRock
[windows]
clear-cache-dev:
	rm -r -fo ./build/testuserdir/var/cache/

# Clears the cache of production versions of ThinkingRock
[linux]
clear-cache-prod:
	rm -rI ~/.cache/trgtd/

# Clears the cache of production versions of ThinkingRock
[macos]
clear-cache-prod:
	rm -rI ~/Library/Caches/trgtd/

# Clears the cache of production versions of ThinkingRock
[windows]
clear-cache-prod:
	rm -r -fo env_var('APPDATA')\..\Local\trgtd\Cache\

# Starts the ThinkingRock application from sources
run:
	ant run-dev
