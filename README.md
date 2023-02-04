# SimpleDistributedSystem

CS335 - Networks - Assignment

The WebServerSystem is a system where multiple Web Servers cooperate among themeselves in order to serve the clients 


![Alt text](assets/images/web_server_team.png?raw=true "Figure 1: WebServer team / Client interaction")

A group of processes, which act as a system of cooperative Web servers, interact with:
-with clients (via HTTP commands) – a client should be able to
communicate with any process from the group of Web servers
- among themselves to save and to search for objects. 
Each Web server must keep a copy of each object that Web clients write to them.

Implemented Events:

**Organization of the group of Web servers**

**Health check of the group of Web Servers**

**Updating the Web servers team**

**Search for objects in the group of Web servers**
