# whatsmytask

A simple tool to track personal activities.

Each activity is managed as a little chat. User can create activity and add sub-activities.

The program saves each post in a sqlite database. It's written in **Java** and uses the
**sqlite-jdbc** library to manage the database.


## Installation

 1. Download latest **sqlite-jdbc** jar file: (https://github.com/xerial/sqlite-jdbc/releases/latest)[https://github.com/xerial/sqlite-jdbc/releases/latest]
 2. Rename the jar file to **whatsmytask.jar**
 3. Prepare the sqlite database
 4. *make*
 5. Run program: "java -cp whatsmytask.jar WhatsmyTask" to run the program


## Run whatsmytask in Linux

The **whatsmytask** script is a perl wrapper to run the **java** program.

To create a symbolic link in */usr/local/bin* run the commands below as superuser:

~~~~
cd /usr/local/bin
ln -s /path/to/whatsmytask
~~~~


## Prepare sqlite database

Run the command to create the database:

~~~~
sqlite3 whatsmytask.sqlite
~~~~

To create the table ised to save the tasks:

~~~~
CREATE TABLE project ( postdate bigint primary key, mainpost bigint not null,
title varchar(40) not null, info varchar(1000), deadline date not null, priority char(1) not null );
~~~~


## Usage

The program is very simple:

~~~~
==============================
### WhatsmyTask 2021.11.04 ###
==============================
Print this help:
WhatsmyTask -h

List open projects ordered by priority and deadline:
WhatsmyTask

List open projects ordered by last modified posts:
WhatsmyTask L

List closed projects:
WhatsmyTask C

List post added during the last N days:
WhatsmyTask W <days>

Insert a new project:
WhatsmyTask I

Insert another job to the project
WhatsmyTask I <jobID>

Insert a new TODO or a TODO to the project
WhatsmyTask T [jobID]

View the TODO List
WhatsmyTask VT

Show a project:
WhatsmyTask S <jobID>

Update a project:
WhatsmyTask U <jobID> <field>=<string>

     field is: title, info, deadline, priority ( 'U', 'N', 'L' )

Close a project:
WhatsmyTask C <jobID>

Delete a project:
WhatsmyTask D <jobID>

Start the shell mode:
WhatsmyTask -I

Exit from shell mode:
WhatsmyTask exit
~~~~

## License

Copyright 2021 Luciano Xumerle

The programs are free software: you can redistribute it and/or modify it under the terms of
the GNU General Public License as published by the Free Software Foundation, either version
3 of the License, or (at your option) any later version.

Please read the LICENSE file.
