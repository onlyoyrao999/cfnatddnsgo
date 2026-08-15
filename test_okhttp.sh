#!/bin/bash
# A simple curl test to see how cdn-cgi/trace behaves with an IP
curl -sI -H "Host: speed.cloudflare.com" https://1.1.1.1/cdn-cgi/trace
