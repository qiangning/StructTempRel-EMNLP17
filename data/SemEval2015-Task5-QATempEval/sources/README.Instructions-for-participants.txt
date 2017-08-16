Evaluation is separated in 3 independent test-sets, one for each domain (news, wikipedia, and blogs). Participants are required to annotated the source test documents in TimeML with their systems. It is important that they make sure they system just add TimeML tags to the input documents and not any extra character (e.g., spaces, newlines).


INPUT Format:
---------------------
sources/
    news/
        file1.TE3input, ..., fileN.TE3input
    wikipedia/
        file1.TE3input, ..., fileN.TE3input
    blogs/
        file1.TE3input, ..., fileN.TE3input


SUBMISSION Format:
--------------------------------
annotations/
    news/
    sysname-runame/
        file1.tml, ..., fileN.tml
...
    wikipedia/
    sysname-runame/
        file1.tml, ..., fileN.tml
...
    blogs/
       sysname-runame/
        file1.tml, ..., fileN.tml
...


Participants can submit as many runs as they want (min: 1, max: 5)
