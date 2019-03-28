# rxh2
A native reactive http/2 server and client implementation for the JVM

## Goals

- Natively reactive, based on RxNetty
- Focus on scalability and optimal resource usage
- http/2 RFC correctness in both client and server
- http/2 multiplexing allowing canceling of streams and flow control to handle backpressure
- Support for streaming of content
- Generic test and benchmark suite to test http/2 clients and servers for correctness and performance

## Non Goals

- Servlet API compatibilty
- http/1.x support
- Support for java <= 1.8
