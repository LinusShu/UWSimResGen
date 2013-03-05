=============================
UW Simulator Result Generator
=============================


----------
How to Use
----------

Get the tutorial video (no audio) to see all of the features in action.

Tutorial Video Link: http://www.industrycorp.ca/UWSimResGenTutorial.zip

Important Notes for getting started:

- you need a config file, see root/config/uwsimgenres_config.xml, and follow it's format
- you need a blocks file, see root/config/uwsimgenres_blocks.xml, and follow it's format
- the blocks file is where you specify the parameters for you run, UNLESS you want to generate a result set containing EVERY combination of reel stops, in which case, you can specify the number of lines and check a box in the application and you will not require a blocks file.
- read the Folder Structure below
- watch the video as it contains a lot of visual detail about the program features



/////////////////////
/* VERSION HISTORY */
/////////////////////

===========
Version 1.0
===========

Features:
- Generate spin results using a combination of a config file and a blocks file
- Config file allows you to configure the attributes of the slot machine simulation
- Blocks file allows you to program blocks of spins that share attributes including: number of spins, number of lines, line bet, and denomination
- Generate spin results for EVERY combination of reel stops
- Generate multiple spin result sets without restarting the program
- Embedded Derby DB
- SQL DB Viewer with DbVisualizer (http://www.dbvis.com/)
- Able to specify your own DB name before each result set generated (No application restart required)
- Able to specify your own Table prefix before each result set generated (No application restart required)
- Logging ( 1 log file per application run, ie: when you launch the application, a log file is started, then every logged action will go to this file until you close the application )
- Real-time updates of result set production progress
- Pause and Resume production
- Cancel production
- Results sent to database immediately to minimize data loss ( You should almost certainly never lose results generated - small batches of 10000 results sent at a time )
- Results generated and delivered to database at a virtually consistent rate. A million results takes about 1 minute to generate and post to the database (Note: Speed will vary depending on your machine. Also we have tested creating a result set as big as 50,000,000 without failure and it took roughly 1 hour to generate )
- Database automatically created if it doesn't already exist ( after you click generate results )

----------------
Folder Structure
----------------
root/config/ - your configs don't have to be here, but it is the first place the program looks to find them, so it's more or less a convenient location

root/logs/ - logs are stored here, the naming convention is log_month_day_year_hour_minute_second_millisecond

root/derby.txt - a log file for the derby database ( this is 3rd party log from Derby )

root/README.txt - this file

root/UWSimResGen_v#_#.jar - the result generator tool, click it to launch the application

---------------------------
Database Naming Conventions
---------------------------

Database Name - what you specify in the application
Table Name - the prefix you specify followed by _month_day_year_hour_minute_second_millisecond


//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////

