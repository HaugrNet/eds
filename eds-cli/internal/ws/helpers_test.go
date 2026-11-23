package ws

import (
	"bufio"
	"io"
	"net"
)

func newReader(conn net.Conn) *bufio.Reader { return bufio.NewReader(conn) }

func readFull(conn net.Conn, buffer []byte) (int, error) { return io.ReadFull(conn, buffer) }
