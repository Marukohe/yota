# Yota: Yet anOther Testing toolkit for Android

## Usage

```
$ ./yota
Yota: yet another testing toolkit for Android

--
help: show help message

usage:
  yota help [command]  show command help message

--
server: start server at 6659 and accept commands

usage: 
  yota server   run server in foreground
  yota serverd  run server as daemon

--
dump: dump ui hierarchy to an output file

usage:
  yota dump
    [-c|--compressed]
    [-b|--background]
    [-m|--meta KVs]
    -o|--output <OUTPUT_FILE|stdout|stderr> 

notes:
  KVs  comma separated key=value pairs, e.g., key1=value1,key2=value2
       these values will be added as attributes to the rooted tag

--
input: an adb-input alike tool

usage: 
  yota input tap -x <x> -y <y>
  yota input longtap -x <x> -y <y>
  yota input swipe --from-x <from-x> 
                   --from-y <from-y> 
                   --to-x <to-x> 
                   --to-y <to-y> 
                   --duration <ms>
  yota input key <key>
  yota input text <text>
  yota input view --type <tap|longtap>
                  [--idx <index>]
                  [--cls <cls>] [--cls-matches <regexp>]
                  [--pkg <pkg>] [--pkg-matches <regexp>]
                  [--txt <text>] [--txt-matches <regexp>]
                  [--txt-contains <str>] [--txt-contains-ignore-case <str>]
                  [--txt-starts-with <str>] [--txt-ends-with <str>]
                  [--desc <desc>] [--desc-matches <regexp>]
                  [--desc-contains <str>] [--desc-contains-ignore-case <str>]
                  [--desc-starts-with <str>] [--desc-ends-with <str>]
                  [--res-id <id>] [--res-id-matches <regexp>]
                  [--res-id-contains <str>] [--res-id-contains-ignore-case <str>]
                  [--clickable] [--long-clickable]
                  [--checkable] [--checked]
                  [--focusable] [--focused]
                  [--scrollable]
                  [--selected]
                  [--enabled]
                  [--dx <dx> --dy <dy> --steps <steps>]

--
select: select views by attributes

usage: 
  yota select [-n <n>]
              [--idx <index>]
              [--cls <cls>] [--cls-matches <regexp>]
              [--pkg <pkg>] [--pkg-matches <regexp>]
              [--txt <text>] [--txt-matches <regexp>]
              [--txt-contains <str>] [--txt-contains-ignore-case <str>]
              [--txt-starts-with <str>] [--txt-ends-with <str>]
              [--desc <desc>] [--desc-matches <regexp>]
              [--desc-contains <str>] [--desc-contains-ignore-case <str>]
              [--desc-starts-with <str>] [--desc-ends-with <str>]
              [--res-id <id>] [--res-id-matches <regexp>]
              [--res-id-contains <str>] [--res-id-contains-ignore-case <str>]
              [--clickable] [--long-clickable]
              [--checkable] [--checked]
              [--focusable] [--focused]
              [--scrollable]
              [--selected]
              [--enabled]

--
mnky: a monkey-like tool that removes redundant events

usage: 
  mnky [-P POLICY]
       [-s SEED]
       [-a MAIN_ACTIVITY]
       [--throttle THROTTLE]
       [--attr-path LENGTH]
       [--pct-last-page PCT]
       [--pct-enter-after-text PCT]
       [--only-alnum]
       [--save-state]
       [--save-screenshot]
       [--show-timestamp]
       [--show-activity]
       [--stop-on-exit]
       -p APP_PACKAGE
       -C COUNT

arguments:
  -p APP_PACKAGE  package name of app under test
  -C COUNT        number of events to be fired

options:
  -P POLICY                  one of {random, dfs}, by default, random
  -s SEED                    seed, by default, system time
  -a MAIN_ACTIVITY           main activity to be launched, by default, 
                             mnky will find one with ACTION_MAIN+CATEGORY_LAUNCHER
  --throttle THROTTLE        interval (ms) between events, by default, 0
  --attr-path LENGTH         show attribute path of length LENGTH of each widget
                             instead of widget information (classname, index, text, ...),
                             by default, LENGTH is 0, i.e., show widget information
  --pct-last-page PCT        percent navigating back to last page, i.e., press BACK,
                             requiring an integer (<100), by default, 10
  --pct-enter-after-text PCT percent pressing enter after sending text event,
                             requiring an integer (<100), by default, 75
  --only-alnum               use only alphabet and number when fuzz string,
                             by default, yes
  --save-state               save state after each event, saved states
                             are saved in directory /data/local/tmp,
                             by default, state will not be saved
  --save-screenshot          save screenshot, only enabled with --save-state
  --show-timestamp           show timestamp (ms) before events and states,
                             by default, don't show
  --show-activity            show activity name of each fired events,
                             by default, don't show
  --stop-on-exit             stop mnky when app exit, either exited normally, or
                             exited by send key BACK, by default, don't stop
```

## Debug

### Step1: start the debugger

If you are using Android 8 or below:

```
$ YOTA_DEBUG_USE_OLD=true YOTA_DEBUG=true ./yota [...]
```

Otherwise:

```
$ YOTA_DEBUG=true ./yota [...]
```

### Step2: forward the debugger

Above commands will start a debugger using JDWP on port 5005 on 
the device, then forward that port to the desktop:

```
$ adb forward tcp:5005 tcp:5005
```

### Step3: set breakpoints and connect to the debugger

Set your breakpoints in Android Studio.
 
Then in the menu: _Run_ > _Debug_ > _Edit configurations..._. 
Click on `+` > _Remote_, and fill the following:

+ Host: `localhost`
+ Port: `5005`

Then click _Debug_.