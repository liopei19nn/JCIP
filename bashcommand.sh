# where is the terminfo
cd /usr/share/terminfo
# decode binary code info
#filename found in $TERM
infocmp filename

: '
/ 				The root of the virtual directory. Normally, no files are placed here. 
/bin 			The binary directory, where many GNU user-level utilities are stored. 
/dev 			The device directory, where Linux creates device nodes.
/etc 			The system configuration files directory.
/lib 			The library directory, where system and application library files are stored.
/opt 			The optional directory, often used to store optional software packages.
/sbin 		The system binary directory, where many GNU admin-level utilities are stored. 
/tmp 			The temporary directory, where temporary work files can be created and destroyed. 
/usr 			The user-installed software directory.
/var 			The variable directory, for files that change frequently, such as log files.
'

# display a / after directory to distinguish directory and file
# executable file will have a *
ls -F

# recursively list in current directory
ls -R

: '
	The file type (such as directory (d), file (-), character device (c), or block device (b)
	The permissions for the file (see Chapter 6)
	The number of hard links to the file (number of links to the same inode)
	The username of the owner of the file
	The group name of the group the file belongs to
	The size of the file in bytes
	The time the file was modified last
	The file or directory name
'
ls -l

# for each file `, print the file's file serial number (inode number)
# The inode of a file or directory is a unique identification number the kernel assigns to each object in the filesystem. 
ls -i

# human readable 
ls -h

# sort the output by file extension
ls -X

# sort the output by file size
ls -S

# sort by file modification time
ls -t 

# ? will match a charactre, * will match 0 to infinite character
ls -l mypro?
ls -l mypro*



# create new file
touch

# create a backup of each existing destination file instead of overwriting it.
cp -b

# recursively copy directory
cp -R

# force overwrite on copy
cp -f

# preserve file attributes
cp -p

# hard link : link to inode: you have two copy of same inode file, must be on same physical medium
# soft link : link to the file symbol: you still have only 1, can be on different medium
#             file size is smaller because you do not need store whole thing. And file inode number is different,
#             means it is a new file
#							when you delete or rename the source file, the soft link file will get lost

# make hard link
ln filename newfilename
# soft link
ln -s filename newfilename


# rm with request confirmation, it will override the -f
rm -i

# show stat
stat filename
# show filetype
file filename

# display file by line number, counting blank line
cat -n filename

# display file by line number, do not count blank line
cat -b filename

# display and compress multiple blank line into 1
cat -s filename

# display without tab charactre, tap will be changed to "^I"
cat -T filename


# use more to read file as vim
more filename
: '
H 						Display a help menu.
spacebar 			Display the next screen of text from the file.
z 						Display the next screen of text from the file.
ENTER 				Display one more line of text from the file.
d 						Display a half-screen (11 lines) of text from the file.
q 						Exit the program.
s 						Skip forward one line of text.
f 						Skip forward one screen of text.
b 						Skip backward one screen of text.
/expression 	Search for the text expression in the file.
n 						Search for the next occurrence of the last specified expression.
′							Go to the first occurrence of the specified expression.
v 						Start up the vi editor at the current line.
=             Display the current line number
'

# less is more powerful on large file
less filename

# following mode for streaming file, only work if the file did not change
# notice editor change will change the file! see inode number change
less +F filename
tail -f filename

# tail following even if the file change name or delete
tail -F filename

: '
tail and head option

-c 					bytes 
-n 					lines 
-f 					Keeps the tail program active and continues to display new lines as they’re added to the file.
--pid=PID  	Along with -f, follows a file until the process with ID PID terminates.
-s sec      Along with -f, sleeps for sec seconds between iterations.
'


: '
	ps command types

	Unix-style parameters, which are preceded by a dash "-"
	BSD-style parameters, which are not preceded by a dash ""
	GNU long parameters, which are preceded by a double dash "--"

'


: "

	# Unix-style ps

	-A 						Show all processes.
	-N 						Show the opposite of the specified parameters.
	-a 						Show all processes except session headers and processes without a terminal.
	-d 						Show all processes except session headers.
	-e 						Show all processes.
	-C cmslist 		Show processes contained in the list cmdlist.
	-G grplist 		Show processes with a group ID listed in grplist.
	-U userlist 	Show processes owned by a userid listed in userlist.
	-g grplist  	Show processes by session or by groupid contained in grplist.
	-p pidlist  	Show processes with PIDs in the list pidlist.
	-s sesslist 	Show processes with session ID in the list sesslist.
	-t ttylist  	Show processes with terminal ID in the list ttylist.
	-u userlist 	Show processes by effective userid in the list userlist.
	-O format 		Display specific columns in the list format, along with the default columns.
	-M 						Display security information about the process.
	-c            Show additional scheduler information about the process.
	-f            Display a full format listing.
	-j            Show job information.
	-l            Display a long listing.
	-o format     Display only specific columns listed in format.
	-y            Don’t show process flags.
	-Z            Display the security context information.
	-H            Display processes in a hierarchical format (showing parent processes).
	-n namelist   Define the values to display in the WCHAN column.
	-w            Use wide output format, for unlimited width displays
	-L            Show process threads
	-V            Display the version of ps
 
 	useful header from \"ps -ef\"

 	UID: The user responsible for launching the process
	PID: The process ID of the process
	PPID: The PID of the parent process (if a process is started by another process)
	C: Processor utilization over the lifetime of the process
	STIME: The system time when the process started
	TTY: The terminal device from which the process was launched
	TIME: The cumulative CPU time required to run the process
	CMD: The name of the program that was started

	more from \" ps -l \"
	F: System flags assigned to the process by the kernel
	S: The state of the process (O = running on processor; S = sleeping; R = runnable, waiting to run; Z = zombie, process terminated but parent not available; T = process stopped)
	PRI: The priority of the process (higher numbers mean lower priority)
	NI: The nice value, which is used for determining priorities
	ADDR: The memory address of the process
	SZ: Approximate amount of swap space required if the process was swapped out
	WCHAN: Address of the kernel function where the process is sleeping

"

: "
 BSD-style syntax

T 					Show all processes associated with this terminal. 
a 					Show all processes associated with any terminal. 
g 					Show all processes including session headers. 
r 					Show only running processes.
x   				Show all processes, even those without a terminal device assigned. 
U userlist 	Show processes owned by a userid listed in userlist.
p pidlist		Show processes with a PID listed in pidlist.
t ttylist		Show processes associated with a terminal listed in ttylist.
O format 		List specific columns in format to display along with the standard columns. 
X 					Display data in the register format.
Z 					Include security information in the output.
j 					Show job information.
l 					Use the long format.
o format 		Display only columns specified in format.
s 					Use the signal format.
u 					Use the user-oriented format.
v 					Use the virtual memory format.
N namelist  Define the values to use in the WCHAN column.
O order     Define the order in which to display the information columns.
S  					Sum numerical information, such as CPU and memory usage, for child processes into the parent process.
c						Display the true command name (the name of the program used to start the process).
f						Display any environment variables used by the command.
h						Display processes in a hierarchical format, showing which processes started which processes.
k sort			Don’t display the header information.
n						Define the column(s) to use for sorting the output.
w						Use numeric values for user and group IDs, along with WCHAN information. Produce wide output for wider terminals.
H						Display threads as if they were processes.
m						Display threads after their processes.
L						List all format specifiers.
V						Display the version of ps.

extra header

VSZ: The size in kilobytes of the process in memory
RSS: The physical memory that a process has used that isn’t swapped out
STAT: A two-character state code representing the current process state

stat 
first character is the same meaning as Linux state, the second has more meaning.

<: The process is running at high priority.
N: The process is running at low priority.
L: The process has pages locked in memory.
s: The process is a session leader.
l: The process is multi-threaded.
+: The process is running in the foreground.

"
top 
: "
	top : realtime monitoring system status

	load average on the system : 1-min, 5-min, 15-min, 15-min is high may indicate system in trouble

"

kill -9 pid
: " 
	kill : process signal
	1 HUP: Hang up.
	2 INT: Interrupt.
	3 QUIT: Stop running.
	9 KILL: Unconditionally terminate.
	11 SEGV : Segment violation.
	15 TERM: Terminate if possible.
	17 STOP: Stop unconditionally, but don’t terminate.
	18 TSTP: Stop or pause, but continue to run in background.
	19 CONT: Resume execution after STOP or TSTP.

"

# list all mounted media
mount

: "
	The device location of the media
	The mount point in the virtual directory where the media is mounted
	The filesystem type
	The access status of the mounted media
"

# mount a device to system
mount -t type device directory

# remove a media
unmount

# show free disk usage
df

# show disk usage of a specific directory
du

: "
	-c: Produce a grand total of all the files listed.
	-h: Print sizes in human-readable form, using K for kilobyte, M for megabyte, and G for gigabyte.
	-s: Summarize each argument.
"

# sort operation on file
# default is sort by character
sort filename
# sort by number
sort -n filename
# sort by field
sort -t "delimeter" -k fieldnumber filename
# sort in descending order in as number
sort -nr filename


: "

	-b			Ignore leading blanks when sorting.
	-C			Don’t sort, but don’t report if data is out of sort order.
	-c			Don’t sort, but check if the input data is already sorted. Report if not sorted.
	-d			Consider only blanks and alphanumeric characters; don’t consider special characters.
	-f			By default, sort orders capitalized letters first. This parameter ignores case.
	-g			Use general numerical value to sort.
	-i			Ignore nonprintable characters in the sort. Sort based on position POS1, and end at
	-k			POS2 if specified.
	-M			Sort by month order using three-character
	-m			month names.
	-n			Sort by string numerical value.
	-o			Write results to file specified.
	-R			--random-sort	 Sort by a random hash of keys.
					--random-source=FILE 	Specify the file for random bytes used by the -R parameter.
	-r      Reverse the sort order (descending instead of ascending.
	-S			Specify the amount of memory to use. Disable last-resort comparison.
	-s			Disable last-resort comparison.
	-T			Specify a location to store temporary working files.
	-t			Specify the character used to distinguish key positions.
	-u			With the -c parameter, check for strict ordering; without the -c parameter, output only the first occurrence of two similar lines.
	-z		  End all lines with a NULL character instead of a newline.	

"

# use grep to understand file
grep pattern filename
# find reverse match
grep -v pattern filename
# find match with line number
grep -n pattern filename
# find count of pattern match
grep -c pattern filename
# find more than one pattern use "e"
# implicit "or" logic
grep -e pattern1 -e pattern2 file1


: "
	file compressiong

	bzip2				Uses the Burrows-Wheeler block sorting text compression algorithm and Huffman coding
	compress		Original Unix file compression utility; starting to fade away into obscurity
  gzip				The GNU Project’s compression utility; uses Lempel-Ziv coding
	zip					The Unix version of the PKZIP program for Windows

"

# use tar to archive file

tar -command outputfile inputfiles

: "

	tar command

	-A					Append an existing tar archive file to another existing tar archive file.
	-c					Create a new tar archive file.
	-d					--diff 		Check the differences between a tar archive file and the filesystem.
							--delete 	Delete from an existing tar archive file.
	-r					Append files to the end of an existing tar archive file. 
	-t					List the contents of an existing tar archive file.
	-u					Append files to an existing tar archive file that are newer than a file with the same name in the existing archive.
	-x					Extract files from an existing archive file.
	-C dir			Change to the specified directory.
	-f file			Output results to file (or device) file.
	-j		      Redirect output to the bzip2 command for compression. 
	-p 					Preserve all file permissions.
	-v					List files as they are processed.
	-z					Redirect the output to the gzip command for compression.

"
#print env variable
printenv

#local env variable
set

#enter child process of bash
bash

#set local variable to global variable
export var="var"

#removing env var
unset var


: '

	Bash Shell Bourne Variable
	echo $PATH : show the dir shell looks for commands
	$HOME : The current user home directory
	$PS1: The primary shell command line interface prompt string.
	$PS2: The secondary shell command line interface prompt string.
'

: '
	Bash Shell Env Variable

	$PPID: The process ID (PID) of the bash shell’s parent process.
  $PWD: The current working directory.
  $RANDOM: Returns a random number between 0 and 32767. Assigning a value to this variable seeds the random number generator.

'

: '
	Login Shell

	Bash will look into the login file in the order 
	/etc/profile
	$HOME/.bash profile
	$HOME/.bash login
	$HOME/.profile

	when you command "bash", you enter interactive shell, which will not process /etc/profile, but will checks .bashrc
'
# variable array
mytest=(one two three four five)
echo $mytest # will only show the first element here
echo ${mytest[0]}
echo ${mytest[*]} # will show the entire array



: '
	vim command 

	x 			#Delete the character at the current cursor position.
	dd 			#Delete the line at the current cursor position.
	dw 			#Delete the word at the current cursor position.
	d$ 			#Delete to the end of the line from the current cursor position.
	J 			#Delete the line break at the end of the line at the current cursor position. Append data after the current cursor position.
	a 			#Append data to the end of the line at the current cursor position.
	r char 	#Replace a single character at the current cursor position with char.
	R text  #Overwrite the data at the current cursor position with text, until you press Escape.
	p 			#paste data, if you delete a char, word, or line, it will be saved to buffer, and you can paste them
	y       #copy == yank, in visual mode select and then paste it
	
	:s/old/new/g 		# to replace all occurrences of old in a line
	:#,#s/old/new/g # to replace all occurrences of old between two line numbers
	:%s/old/new/g 	# to replace all occurrences of old in the entire file  
	:%s/old/new/gc  #to replace all occurrences  of old in the entire file, but prompt for each occurrences


'




























