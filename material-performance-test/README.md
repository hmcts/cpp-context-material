To run performance test from your laptop against the material context validation environment:

- ssh projectadm@10.124.22.19 -L 5432:localhost:5432
- Amend these two entries in pom.xml, to point at material context validation:

<target.host>10.124.22.19</target.host>
<target.jdbc.url>jdbc:postgresql://localhost:5432/postgres</target.jdbc.url>

- Enable proxy nodes in JMeter test plan
