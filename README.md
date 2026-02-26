# Baseless


### This repo holds two projects:

1. A java webserver from scratch, without the Java standard library. 
IO, collections, and other logic was made from scratch using `libc` 
function calls with java Foreign Memory Access.


2. An end-to-end encrypted file/text sharing service between devices,
designed for when you don't particularly trust the server


---


## Learning experiences

- Java foreign memory & function access.
- Lower levels of AES encryption (IV, salt etc)
- In-depth knowledge of exactly how HTTP1.x works down to the bytes.
- Regex engine/matching
- A good refresher on libc for sockets, and how structs are stored in memory (padding etc)
- Java shared thread access on arenas
