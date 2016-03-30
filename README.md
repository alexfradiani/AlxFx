# AlxFx
Java-based custom local back-testing platform for foreign exchange trading.

# Description
This app was created to have a custom backtesting system for evaluating fx strategies.

It is based on JForex platform from Dukascopy.
java source code is in `project` folder.
html customized report web code is in `JFExtendedReports` folder.

#### How it works
Classes in fradiani.env establish the communication with tick data loaded from csv files (this should be improved to work with a db and reduce space size) that for simplicity of manipulation emulate the tick data that can be publicly downloaded from dukascopy servers.

`TickServer` is in charge of loading the tick data and serve it to the `TestingEnv`, it uses the following logic:
* Chunks of string lines are read from the csv files an kept in cache
* The lines of the pairs are ordered serving those that are closer in time.
* When a chunk is completed, another one is loaded until the data of a particular day is done.

`TestingEnv` is the platform execution environment, run this class to test a particular strategy.

The testing environment class loads a strategy in order to execute its specific trading rules, the global parameters for a backtest are defined in
`fradiani.strategies.ALX_COMMON` i.e: `periodStart`, `periodEnd`, `equity`, `strategyName`,  where `strategyName` is the full class name of the strategy 
to be executed.

# Configuration

In order to be able to run tests, the following things need to be set:
* In `TickServer`, edit the path to load CSV tick data
	
	`public static final String TICK_PATH = "/path/to/csv_data/";`

the structure for storing the files is `PAIR/YEAR/PAIR_YEAR_MONTH_DAY.csv`, e.g: EURUSD/2013/EURUSD_2013_03_01.csv
the csv data can be obtained directly from Dukascopy or using tools like Tick Data Downloader.

* in `TestingEnv`, the path for the reporting must point to the JFExtendedReports folder, this is the html/js code that shows the performance report of strategies tested.
	
	`public static final String REPORTS_PATH = "/path/to/AlxFx/JFExtendedReports/public_html/";`

#### Creating Strategies
The `SampleTrades` class could be used as template and as guide to understand the life-cycle of a strategy, which is very similar to the way strategies are defined in the JForex platform. A couple of simple technical indicators are defined in the `fradiani.indicators` package, but they are at a very primitive stage and should be improved.