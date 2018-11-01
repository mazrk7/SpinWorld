# SpinWorld #

This project is a multi-agent implementation of a system of retributive justice that uses the agent-based Java simulator, [Presage2](https://github.com/Presage/Presage2), coupled with the integrated [Drools](https://www.drools.org/) rule engine. Please cite the following work if you make use of this repository:

```
@inproceedings{Zolotas2016,
author = {M. Zolotas and J. Pitt},
title = {Self-Organising Error Detection and Correction in Open Multi-agent Systems},
booktitle = {IEEE 1st International Workshops on Foundations and Applications of Self* Systems (FAS*W)},
year = {2016},
month = {Sept},
pages = {180-185}
}
```

The testbed used for experimentation with this system was the LPG' game, which is a variant of the Linear Public Good game, as described in the following works:

* Jeremy Pitt, Julia Schaumeier, Didac Busquests and Sam Macbeth, "Self-Organising Common-Pool Resource Allocation and Canons of Distributive Justice" SASO 2012.
* Jeremy Pitt and Julia Schaumeier, "Provision and Appropriation of Common-Pool Resources without Full Disclosure" PRIMA 2012.

## Usage ##

In order to run this project, both [maven](http://maven.apache.org/) and a JDK must be installed.

A complete and introductory guide for setting up projects in the Eclipse IDE to use with Presage2 is available at the following link:
http://www.presage2.info/w/Getting_Started_Guide.

This link will hopefully provide more general information for getting started with Presage2, which may assist with this particular project's setup.

Additional dependencies are:

* JUNG 2.0.1 for network visualisation. Package available at: http://jung.sourceforge.net/
* JFreeChart 1.0.19 for graph plotting. Package available at: http://www.jfree.org/jfreechart/

Other sources of code that influenced development:

* [LPG'](https://github.com/sammacbeth/LPG--Game) for developing the LPG' game as a testbed.
* [Patches](https://github.com/mkrauskopf/jfreechart-patches) for radar/spider chart plotting in the GUI section of the project.

## Database Configuration ##

All simulation data is stored into a PostgreSQL database (>= v9.1) and requires the hstore extension. A src/main/resources/db.properties file should be added to the project source directory and should include login details for database setup. See the [presage2-sqldb](https://github.com/Presage/presage2-sqldb) docs for full configuration options.

## Command Line Interface (CLI) usage ##

The spinworld-cli script is an alias of mvn exec:java, which offers the user the ability to add and run simulations. Navigate to the project directory and run the script with no arguments in order to view all available commands: 

cd /path/to/SpinWorld

./spinworld-cli

There are a few different experiments provided via the 'insert' command. This command will insert a set of simulations with parameters configured according to the specified experiments undergoing evaluation. A number of repeats to run for the experiment must be provided as a command line argument and a starting random seed may also be optionally assigned. For example, 7 repeats of the 'graduated_sanctions' experiment with seeds starting at 21 would be run as follows:

./spinworld-cli insert graduated_sanctions 7 --seed 21

This will insert the simulations into the database. To actually run these simulations from the database, the runall command should be used. This runs all simulations which have yet to be executed.
