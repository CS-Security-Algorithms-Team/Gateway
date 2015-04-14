# Gateway
Gateway for networking for anonophant

yyyy-MM-dd'T'HH:mm:ss  - Third to last example on the SimpleDateFormat page. Removed the zone and we will just use UTC. (Can't use Java 8 time because CSX only has Java 7).

**Our protocol**

Expecting literally (for example) "NEW_CONTENT" followed by a new line.

Dates for tokens SimpleDateFormat pattern 

NEW_CONTENT
<token>
<media request id>

OLD_CONTENT
<old token>
<new token>

Returned formats

REQUEST_OK
<content>

TOKEN_INVALID

TOKEN_EXPIRED
