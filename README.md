## Description

This is a pedestal-service port of sinatra's [sse
chat example](https://github.com/sinatra/sinatra/blob/master/examples/chat.rb).
This chat can be used from multiple tabs in the same browser. For
official sse samples, see
[server-sent-events](https://github.com/pedestal/samples/tree/master/server-sent-events)
and [chat](https://github.com/pedestal/samples/tree/master/chat).

## Getting Started

1. Start the application: `lein run`
2. Go to [localhost:8080](http://localhost:8080/) to start chatting.

## Configuration

To configure logging see config/logback.xml. By default, the app logs to stdout and logs/.
To learn more about configuring Logback, read its [documentation](http://logback.qos.ch/documentation.html).

## Limitations
Removing subscribers doesn't fully work so occasionally heartbeats
from nonexistent users will clutter error logs.
