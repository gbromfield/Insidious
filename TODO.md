# TODOs

1. <s>allow ctrl-d to close the ssh session.</s>
2. <s>make sure threads cleaned up on session removal.</s>
3. <s>move the SSH key file to the executable jar file.</s>
4. <s>allow more than one recording to be specified so that constants like act-user and rtrv-hdr don't need to be in the recording everytime.</s>
5. <s>AO multiplicity, Update the ATAG when sending out AOs.</s>
6. When the RA disconnects cleanup.
7. <s>Refactor input arguments.</s>
8. Implement Disconnects (maybe just inserting disconnects rather than scraping from logs is more efficient)
9. <s>Support response continuations in interactive mode.</s>
10. Scaleability - 2 threads are created per connection. Only 1 or a small number of connections required so not currently a problem.
11. <s>REST API to log the command DB</s>
12. Add CLI
13. Add specification of the response to use when inputs exhausted
14. Add swagger ui
15. Add error reporting/logging on loading of recordings.
