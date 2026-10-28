# Baseless


### This repo holds two projects:

1. A java webserver from scratch, without the Java standard library. 
IO, collections, and other logic was made from scratch using `libc` 
function calls with java Foreign Memory Access.


2. An end-to-end encrypted file/text sharing service between devices,
designed for when you don't particularly trust the server


---


## Setup

Install **NEW** versions of java/javac, kotlin/kotlinc, npm and nodejs, and make sure they're on your path.
I suggest (I recommend sdkman on a non-rolling distro like ubuntu)

run `echo PORT=32000 >> .env`

run `cd frontend && npm i`

run `make jar`

Then you can run the server with `run.sh`, in a systemd unit etc



---


## Learning experiences

- Java foreign memory & function access.
- Lower levels of AES encryption (IV, salt etc)
- In-depth knowledge of exactly how HTTP1.x works down to the bytes.
- Regex engine/matching
- A good refresher on libc for sockets, and how structs are stored in memory (padding etc)
- Java shared thread access on arenas

