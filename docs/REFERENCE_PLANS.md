# Reference plans

How do Abysner dive plans compare to dive plans created by other dive planners? Below are some
reference plans. If you want to recreate a reference plan, these are the settings used by all of
them, some settings (Gradient factors, Salinity, Altitude and Last-deco stop) are specific for each
plan, see the plan specific tables for those.


| **Setting**        | **Value**        |
|--------------------|------------------|
| Ascent             | 5 m/min          |
| Descent            | 5 m/min          |
| Algorithm          | Bühlmann ZHL-16C |
| Deco PPO2          | 1.6              |
| Bottom/travel PPO2 | 1.4              |
| END                | 30 meter         |
| O2 Narcotic        | true             |

## Reference plan 1
**20 meter, 20 minutes, single-gas (21/0)**

| GF    | Salinity | Altitude | Last-deco stop |
|-------|----------|----------|----------------|
| 30/70 | Fresh    | 0 meters | 3 meter        |

<details>
<summary>Abysner</summary>

| Depth | Duration | Runtime | Gas  |
|-------|----------|---------|------|
| 20m   | 4min     | 4min    | 21/0 |
| 20m   | 16min    | 20min   | 21/0 |
| 0m    | 4min     | 24min   | 21/0 |
**CNS**: 3%  
**OTU**: 6
</details>

<details>
<summary>Subsurface</summary>

| Depth | Duration | Runtime | Gas  |
|-------|----------|---------|------|
| 20m   | 4min     | 4min    | 21/0 |
| 20m   | 16min    | 20min   | 21/0 |
| 0m    | 4min     | 24min   | 21/0 |
**CNS**: 3%  
**OTU**: 6  
*Subsurface (6.0.5214-CICD-release)*
</details>

<details>
<summary>DIVESOFT.APP</summary>

| Depth | Duration | Runtime | Gas  |
|-------|----------|---------|------|
| 20m   | 4min     | 4min    | 21/0 |
| 20m   | 16min    | 20min   | 21/0 |
| 0m    | 4min     | 24min   | 21/0 |
**CNS**: 2%  
**OTU**: 5  
*DIVESOFT.APP (Android 1.8.4)*
</details>

## Reference plan 2
**30 meter, 30 minutes, multi-gas**

| GF    | Salinity | Altitude | Last-deco stop |
|-------|----------|----------|----------------|
| 30/70 | Salt     | 0 meters | 6 meter        |

<details>
<summary>Abysner</summary>

|   | Depth | Duration | Runtime | Gas  |
|---|-------|----------|---------|------|
| ➘ | 30m   | 6min     | 6min    | 21/0 |
| ➙ | 30m   | 24min    | 30min   | 21/0 |
| ➚ | 21m   | 2min     | 32min   | 21/0 |
| - | 21m   | 1min     | 33min   | 50/0 |
| ➚ | 9m    | 3min     | 36min   | 50/0 |
| ⏹ | 9m    | 1min     | 37min   | 50/0 |
| ⏹ | 6m    | 11min    | 48min   | 50/0 |
| ➚ | 0m    | 2min     | 50min   | 50/0 |
**CNS**: 12%  
**OTU**: 34
</details>

<details>
<summary>Subsurface</summary>

> **Observations:**
> - Subsurface merges the ascent from 21 meter to 9 meter into the 9 meter stop, while Abysner shows
>   the ascent as a separate segment followed by the stop.
> - Subsurface rounds the final ascent (6 to 0 meter) down to 1 minute while the ascent actually
>   takes longer at 5 meter per min. Abysner uses 2 min, adjusting the ascent slope to fit exactly.

|   | Depth | Duration | Runtime | Gas  |
|---|-------|----------|---------|------|
| ➘ | 30m   | 6min     | 6min    | 21/0 |
| ➙ | 30m   | 24min    | 30min   | 21/0 |
| ➚ | 21m   | 2min     | 32min   | 21/0 |
| - | 21m   | 1min     | 33min   | 50/0 |
| ⏹ | 9m    | 3min     | 36min   | 50/0 |
| ⏹ | 6m    | 11min    | 47min   | 50/0 |
| ➚ | 0m    | 1min     | 48min   | 50/0 |
**CNS**: 13%  
**OTU**: 34  
*Subsurface (6.0.5214-CICD-release)*
</details>

<details>
<summary>DIVESOFT.APP</summary>

> **Observations:**
> DIVESOFT.APP does not display ascents between deco stops, and does not include the ascent time
> to the next stop in the total stop duration. This means the displayed stop time does not always
> match up to the runtime. Duration values below were derived by subtracting runtimes and the
> final ascent using the leftover runtime (which is displayed by DIVESOFT.APP).
>
> DIVESOFT.APP does not appear to include a gas switch time. With gas switch time set to zero
> Abysner produces the same total runtime (50min), though the individual stop distributions differ
> ever so slightly.

|   | Depth | Duration | Runtime | Gas  |
|---|-------|----------|---------|------|
| ➘ | 30m   | 6min     | 6min    | 21/0 |
| ➙ | 30m   | 24min    | 30min   | 21/0 |
| ➚ | 21m   | 2min     | 32min   | 21/0 |
| ➚ | 9m    | 2min     | 34min   | 50/0 |
| ⏹ | 9m    | 1min     | 35min   | 50/0 |
| ⏹ | 6m    | 13min    | 48min   | 50/0 |
| ➚ | 0m    | 2min     | 50min   | 50/0 |
**CNS**: 11%  
**OTU**: 32  
*DIVESOFT.APP (Android 1.8.4)*
</details>


## Reference plan 3
**45 meter, 15 minutes, multi-gas, trimix**

| GF    | Salinity | Altitude | Last-deco stop |
|-------|----------|----------|----------------|
| 30/70 | Salt     | 0 meters | 3 meter        |

<details>
<summary>Abysner</summary>

|   | Depth | Duration | Runtime | Gas   |
|---|-------|----------|---------|-------|
| ➘ | 45m   | 9min     | 9min    | 21/35 |
| ➙ | 45m   | 6min     | 15min   | 21/35 |
| ➚ | 21m   | 5min     | 20min   | 21/35 |
| - | 21m   | 1min     | 21min   | 50/0  |
| ➚ | 6m    | 3min     | 24min   | 50/0  |
| ⏹ | 6m    | 2min     | 26min   | 50/0  |
| ⏹ | 3m    | 6min     | 32min   | 50/0  |
| ➚ | 0m    | 1min     | 33min   | 50/0  |
**CNS**: 9%  
**OTU**: 24
</details>

<details>
<summary>Subsurface</summary>

> **Observations:**
> Subsurface merges the ascent from 21 meter to 6 meter into the 6 meter stop, while Abysner shows
> the ascent as a separate segment followed by the stop.

|   | Depth | Duration | Runtime | Gas   |
|---|-------|----------|---------|-------|
| ➘ | 45m   | 9min     | 9min    | 21/35 |
| ➙ | 45m   | 6min     | 15min   | 21/35 |
| ➚ | 21m   | 5min     | 20min   | 21/35 |
| - | 21m   | 1min     | 21min   | 50/0  |
| ⏹ | 6m    | 5min     | 26min   | 50/0  |
| ⏹ | 3m    | 5min     | 31min   | 50/0  |
| ➚ | 0m    | 1min     | 32min   | 50/0  |
**CNS**: 10%  
**OTU**: 26  
*Subsurface (6.0.5214-CICD-release)*
</details>

<details>
<summary>DIVESOFT.APP</summary>

> **Observations:**
> DIVESOFT.APP does not display ascents between deco stops, and does not include the ascent time
> to the next stop in the total stop duration. This means the displayed stop time does not always
> match up to the runtime. Duration values below were derived by subtracting runtimes and the
> final ascent using the leftover runtime (which is displayed by DIVESOFT.APP).
>
> - DIVESOFT.APP does not appear to include a gas switch duration. With the gas switch duration set
>   to one minute Abysner produces the same total runtime of 33 minutes. The individual stop
>   distributions differ ever so slightly.

|   | Depth | Duration | Runtime | Gas   |
|---|-------|----------|---------|-------|
| ➘ | 45m   | 9min     | 9min    | 21/35 |
| ➙ | 45m   | 6min     | 15min   | 21/35 |
| ➚ | 21m   | 5min     | 20min   | 21/35 |
| ➚ | 9m    | 2min     | 22min   | 50/0  |
| ⏹ | 9m    | 1min     | 23min   | 50/0  |
| ⏹ | 6m    | 2min     | 25min   | 50/0  |
| ⏹ | 3m    | 7min     | 32min   | 50/0  |
| ➚ | 0m    | 1min     | 33min   | 50/0  |
**CNS**: 9%  
**OTU**: 23  
*DIVESOFT.APP (Android 1.8.4)*
</details>


## Reference plan 4
**60 meter, 20 minutes, multi-gas, trimix, altitude**

| GF    | Salinity | Altitude    | Last-deco stop |
|-------|----------|-------------|----------------|
| 40/85 | Fresh    | 1000 meters | 3 meter        |

<details>
<summary>Abysner</summary>

|   | Depth | Duration | Runtime | Gas   |
|---|-------|----------|---------|-------|
| ➘ | 60m   | 12min    | 12min   | 18/45 |
| ➙ | 60m   | 8min     | 20min   | 18/45 |
| ➚ | 21m   | 8min     | 28min   | 18/45 |
| - | 21m   | 1min     | 29min   | 50/0  |
| ➚ | 15m   | 2min     | 31min   | 50/0  |
| ⏹ | 15m   | 1min     | 32min   | 50/0  |
| ⏹ | 12m   | 2min     | 34min   | 50/0  |
| ⏹ | 9m    | 4min     | 38min   | 50/0  |
| ⏹ | 6m    | 7min     | 45min   | 50/0  |
| ⏹ | 3m    | 13min    | 58min   | 50/0  |
| ➚ | 0m    | 1min     | 59min   | 50/0  |
**CNS**: 15%  
**OTU**: 41
</details>

<details>
<summary>Subsurface</summary>

> **Observations:**
> - Atmospheric pressure was set to 900 mbar directly in Subsurface to match Abysner's barometric
>   formula result for 1000 meter altitude, eliminating it as a variable in the comparison.
> - The remaining stop-time differences (12, 6 and 3 meter) seem to be algorithmic.

|   | Depth | Duration | Runtime | Gas   |
|---|-------|----------|---------|-------|
| ➘ | 60m   | 12min    | 12min   | 18/45 |
| ➙ | 60m   | 8min     | 20min   | 18/45 |
| ➚ | 21m   | 8min     | 28min   | 18/45 |
| - | 21m   | 1min     | 29min   | 50/0  |
| ⏹ | 15m   | 3min     | 32min   | 50/0  |
| ⏹ | 12m   | 3min     | 35min   | 50/0  |
| ⏹ | 9m    | 4min     | 39min   | 50/0  |
| ⏹ | 6m    | 8min     | 47min   | 50/0  |
| ⏹ | 3m    | 15min    | 62min   | 50/0  |
| ➚ | 0m    | 1min     | 63min   | 50/0  |
**CNS**: 17%  
**OTU**: 46  
*Subsurface (6.0.5576-CICD-release)*
</details>

<details>
<summary>DIVESOFT.APP</summary>

> **Observations:**
> DIVESOFT.APP does not support setting an altitude. This plan has been based on 0 meters instead
> of 1000 meters used in the other planners.
>
> DIVESOFT.APP does not display ascents between deco stops, and does not include the ascent time
> to the next stop in the total stop duration. This means the displayed stop time does not always
> match up to the runtime. Duration values below were derived by subtracting runtimes and the
> final ascent using the leftover runtime (which is displayed by DIVESOFT.APP).

|   | Depth | Duration | Runtime | Gas   |
|---|-------|----------|---------|-------|
| ➘ | 60m   | 12min    | 12min   | 18/45 |
| ➙ | 60m   | 8min     | 20min   | 18/45 |
| ➚ | 21m   | 8min     | 28min   | 18/45 |
| ➚ | 18m   | 0min     | 28min   | 18/45 |
| ⏹ | 18m   | 1min     | 29min   | 50/0  |
| ⏹ | 15m   | 3min     | 32min   | 50/0  |
| ⏹ | 12m   | 2min     | 34min   | 50/0  |
| ⏹ | 9m    | 5min     | 39min   | 50/0  |
| ⏹ | 6m    | 8min     | 47min   | 50/0  |
| ⏹ | 3m    | 17min    | 64min   | 50/0  |
| ➚ | 0m    | 1min     | 65min   | 50/0  |
**CNS**: 17%  
**OTU**: 47  
*DIVESOFT.APP (Android 1.8.4)*
</details>


## Reference plan 5
**40 meter max, multi-level (cave-profile) dive, single-gas trimix**

*Note: this is not meant to be a realistic scenario.*

<details>
<summary>Plan details</summary>

```
In:
- Descent: 40 meter, 8 minutes
- Flat:    40 meter, 2 minutes
- Ascent:  30 meter, 2 minutes
- Flat:    30 meter, 8 minutes
Out:
- Flat: 30 meter, 8 minutes
- Descent: 40 meter, 2 minutes
- Flat: 40 meter, 2 minutes
- Ascent: at 5 m/min max (as planned by planner)
```
</details>

| GF    | Salinity | Altitude | Last-deco stop |
|-------|----------|----------|----------------|
| 50/80 | Fresh    | 0 meters | 3 meter        |

<details>
<summary>Abysner</summary>

|   | Depth | Duration | Runtime | Gas   |
|---|-------|----------|---------|-------|
| ➘ | 40m   | 8min     | 8min    | 21/20 |
| ➙ | 40m   | 2min     | 10min   | 21/20 |
| ➚ | 30m   | 2min     | 12min   | 21/20 |
| ➙ | 30m   | 16min    | 28min   | 21/20 |
| ➘ | 40m   | 2min     | 30min   | 21/20 |
| ➙ | 40m   | 2min     | 32min   | 21/20 |
| ➚ | 9m    | 7min     | 39min   | 21/20 |
| ⏹ | 9m    | 3min     | 42min   | 21/20 |
| ⏹ | 6m    | 7min     | 49min   | 21/20 |
| ⏹ | 3m    | 17min    | 66min   | 21/20 |
| ➚ | 0m    | 1min     | 67min   | 21/20 |
**CNS**: 8%  
**OTU**: 26
</details>

<details>
<summary>Subsurface</summary>

|   | Depth | Duration | Runtime | Gas   |
|---|-------|----------|---------|-------|
| ➘ | 40m   | 8min     | 8min    | 21/20 |
| ➙ | 40m   | 2min     | 10min   | 21/20 |
| ➚ | 30m   | 2min     | 12min   | 21/20 |
| ➙ | 30m   | 16min    | 28min   | 21/20 |
| ➘ | 40m   | 2min     | 30min   | 21/20 |
| ➙ | 40m   | 2min     | 32min   | 21/20 |
| ➚ | 9m    | 7min     | 39min   | 21/20 |
| ⏹ | 9m    | 3min     | 42min   | 21/20 |
| ⏹ | 6m    | 8min     | 50min   | 21/20 |
| ⏹ | 3m    | 16min    | 66min   | 21/20 |
| ➚ | 0m    | 1min     | 67min   | 21/20 |
**CNS**: 9%  
**OTU**: 25  
*Subsurface (6.0.5214-CICD-release)*
</details>

<details>
<summary>DIVESOFT.APP</summary>

> **Observations:**
> DIVESOFT.APP does not display ascents between deco stops, and does not include the ascent time
> to the next stop in the total stop duration. This means the displayed stop time does not always
> match up to the runtime. Duration values below were derived by subtracting runtimes and the
> final ascent using the leftover runtime (which is displayed by DIVESOFT.APP).

|   | Depth | Duration | Runtime | Gas   |
|---|-------|----------|---------|-------|
| ➘ | 40m   | 8min     | 8min    | 21/20 |
| ➙ | 40m   | 2min     | 10min   | 21/20 |
| ➚ | 30m   | 2min     | 12min   | 21/20 |
| ➙ | 30m   | 16min    | 28min   | 21/20 |
| ➘ | 40m   | 2min     | 30min   | 21/20 |
| ➙ | 40m   | 2min     | 32min   | 21/20 |
| ➚ | 9m    | 6min     | 38min   | 21/20 |
| ⏹ | 9m    | 3min     | 41min   | 21/20 |
| ⏹ | 6m    | 7min     | 48min   | 21/20 |
| ⏹ | 3m    | 16min    | 64min   | 21/20 |
| ➚ | 0m    | 1min     | 65min   | 21/20 |
**CNS**: 8%  
**OTU**: 24  
*DIVESOFT.APP (Android 1.8.4)*
</details>


## Reference plan 6 (CCR)
**30 meter, 30 minutes, CCR with air diluent, setpoints 0.7 low / 1.2 high**

| GF    | Salinity | Altitude | Last-deco stop | Low SP | High SP |
|-------|----------|----------|----------------|--------|---------|
| 30/70 | Salt     | 0 meters | 3 meter        | 0.7    | 1.2     |

<details>
<summary>Abysner</summary>

|   | Depth | Duration | Runtime | Gas  | Mode          |
|---|-------|----------|---------|------|---------------|
| ➘ | 30m   | 6min     | 6min    | 21/0 | CCR (SP 0.7)  |
| ➙ | 30m   | 24min    | 30min   | 21/0 | CCR (SP 1.2)  |
| ➚ | 3m    | 6min     | 36min   | 21/0 | CCR (SP 1.2)  |
| ⏹ | 3m    | 2min     | 38min   | 21/0 | CCR (SP 1.2)  |
| ➚ | 0m    | 1min     | 39min   | 21/0 | CCR (SP 1.2)  |
**CNS**: 17%  
**OTU**: 47
</details>

<details>
<summary>Subsurface</summary>

|   | Depth | Duration | Runtime | Gas  | Mode          |
|---|-------|----------|---------|------|---------------|
| ➘ | 30m   | 6min     | 6min    | 21/0 | CCR (SP 0.7)  |
| ➙ | 30m   | 24min    | 30min   | 21/0 | CCR (SP 1.2)  |
| ➚ | 3m    | 6min     | 36min   | 21/0 | CCR (SP 1.2)  |
| - | 3m    | 2min     | 38min   | 21/0 | CCR (SP 1.2)  |
| ➚ | 0m    | 1min     | 39min   | 21/0 | CCR (SP 1.2)  |
**CNS**: 16%  
**OTU**: 46  
*Subsurface (6.0.5576-CICD-release)*
</details>

<details>
<summary>DIVESOFT.APP</summary>

> **Observations:**
> DIVESOFT.APP does not display ascents between deco stops, and does not include the ascent time
> to the next stop in the total stop duration. This means the displayed stop time does not always
> match up to the runtime. Duration values below were derived by subtracting runtimes and the
> final ascent using the leftover runtime (which is displayed by DIVESOFT.APP).
>
> - DIVESOFT.APP displays 1.2 for the setpoint on the initial descent, while 0.7 was configured for
>   descents. It is unclear whether this is just a display choice (labeling the descent with the
>   bottom setpoint) or whether the low setpoint is not applied during descent.

|   | Depth | Duration | Runtime | Gas  | Mode          |
|---|-------|----------|---------|------|---------------|
| ➘ | 30m   | 6min     | 6min    | 21/0 | CCR (SP 1.2)  |
| ➙ | 30m   | 24min    | 30min   | 21/0 | CCR (SP 1.2)  |
| ➚ | 6m    | 5min     | 35min   | 21/0 | CCR (SP 1.2)  |
| ⏹ | 6m    | 1min     | 36min   | 21/0 | CCR (SP 1.2)  |
| ⏹ | 3m    | 3min     | 39min   | 21/0 | CCR (SP 1.2)  |
| ➚ | 0m    | 1min     | 40min   | 21/0 | CCR (SP 1.2)  |
**CNS**: 17%  
**OTU**: 47  
*DIVESOFT.APP (Android 2.5.1)*
</details>


## Reference plan 7 (CCR bailout)
**30 meter, 30 minutes, CCR with air diluent (setpoints 0.7 low / 1.2 high), bailout to open
circuit**

Same configuration as reference plan 6, but the diver bails out to open circuit at the end of
the bottom section.

| GF    | Salinity | Altitude | Last-deco stop | Low SP | High SP |
|-------|----------|----------|----------------|--------|---------|
| 30/70 | Salt     | 0 meters | 3 meter        | 0.7    | 1.2     |

<details>
<summary>Abysner</summary>

|   | Depth | Duration | Runtime | Gas  | Mode          |
|---|-------|----------|---------|------|---------------|
| ➘ | 30m   | 6min     | 6min    | 21/0 | CCR (SP 0.7)  |
| ➙ | 30m   | 24min    | 30min   | 21/0 | CCR (SP 1.2)  |
| - | 30m   | 1min     | 31min   | 21/0 | Bailout to OC |
| ➚ | 9m    | 5min     | 36min   | 21/0 | OC            |
| ⏹ | 9m    | 1min     | 37min   | 21/0 | OC            |
| ⏹ | 6m    | 5min     | 42min   | 21/0 | OC            |
| ⏹ | 3m    | 9min     | 51min   | 21/0 | OC            |
| ➚ | 0m    | 1min     | 52min   | 21/0 | OC            |
**CNS**: 13%  
**OTU**: 37
</details>

<details>
<summary>Subsurface</summary>

> **Observations:**
> Subsurface produces a 1 minute shorter runtime (51 vs 52 minutes) with slightly different stop
> distributions.

|   | Depth | Duration | Runtime | Gas  | Mode          |
|---|-------|----------|---------|------|---------------|
| ➘ | 30m   | 6min     | 6min    | 21/0 | CCR (SP 0.7)  |
| ➙ | 30m   | 24min    | 30min   | 21/0 | CCR (SP 1.2)  |
| - | 30m   | 1min     | 31min   | 21/0 | Bailout to OC |
| ➚ | 9m    | 4min     | 35min   | 21/0 | OC            |
| ⏹ | 9m    | 2min     | 37min   | 21/0 | OC            |
| ⏹ | 6m    | 5min     | 42min   | 21/0 | OC            |
| ⏹ | 3m    | 8min     | 50min   | 21/0 | OC            |
| ➚ | 0m    | 1min     | 51min   | 21/0 | OC            |
**CNS**: 13%  
**OTU**: 37  
*Subsurface (6.0.5576-CICD-release)*
</details>

<details>
<summary>DIVESOFT.APP</summary>

> **Observations:**
> DIVESOFT.APP does not display ascents between deco stops, and does not include the ascent time
> to the next stop in the total stop duration. This means the displayed stop time does not always
> match up to the runtime. Duration values below were derived by subtracting runtimes and the
> final ascent using the leftover runtime (which is displayed by DIVESOFT.APP).
>
> - DIVESOFT.APP produces a significantly longer runtime (60 min vs 52/51 min for Abysner and
>   Subsurface). This is a much larger difference than seen in the OC reference plans. It is unclear
>   why this is, but the DIVESOFT.APP versions between OC and CCR plans differ, could that be a
>   cause? Or is it a difference in how the bailout is handled?
> - DIVESOFT.APP displays 1.2 for the setpoint on the initial descent, while 0.7 was configured for
>   descents. It is unclear whether this is just a display choice (labeling the descent with the
>   bottom setpoint) or whether the low setpoint is not applied during descent.

|   | Depth | Duration | Runtime | Gas  | Mode          |
|---|-------|----------|---------|------|---------------|
| ➘ | 30m   | 6min     | 6min    | 21/0 | CCR (SP 1.2)  |
| ➙ | 30m   | 24min    | 30min   | 21/0 | CCR (SP 1.2)  |
| - | 30m   | 3min     | 33min   | 21/0 | Bailout to OC |
| ➚ | 9m    | 4min     | 37min   | 21/0 | OC            |
| ⏹ | 9m    | 3min     | 40min   | 21/0 | OC            |
| ⏹ | 6m    | 5min     | 45min   | 21/0 | OC            |
| ⏹ | 3m    | 14min    | 59min   | 21/0 | OC            |
| ➚ | 0m    | 1min     | 60min   | 21/0 | OC            |
**CNS**: 12%  
**OTU**: 35  
*DIVESOFT.APP (Android 2.5.1)*
</details>


## Reference plan 8 (CCR)
**60 meter, 20 minutes, CCR with 10/70 trimix diluent, setpoints 0.7 low / 1.2 high**

| GF    | Salinity | Altitude | Last-deco stop | Low SP | High SP |
|-------|----------|----------|----------------|--------|---------|
| 30/70 | Salt     | 0 meters | 3 meter        | 0.7    | 1.2     |

<details>
<summary>Abysner</summary>

|   | Depth | Duration | Runtime | Gas   | Mode         |
|---|-------|----------|---------|-------|--------------|
| ➘ | 60m   | 12min    | 12min   | 10/70 | CCR (SP 0.7) |
| ➙ | 60m   | 8min     | 20min   | 10/70 | CCR (SP 1.2) |
| ➚ | 24m   | 8min     | 28min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 24m   | 1min     | 29min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 21m   | 2min     | 31min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 18m   | 2min     | 33min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 15m   | 3min     | 36min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 12m   | 4min     | 40min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 9m    | 6min     | 46min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 6m    | 9min     | 55min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 3m    | 14min    | 69min   | 10/70 | CCR (SP 1.2) |
| ➚ | 0m    | 1min     | 70min   | 10/70 | CCR (SP 1.2) |
**CNS**: 29%  
**OTU**: 82
</details>

<details>
<summary>Subsurface</summary>

> **Observations:**
> Subsurface does not require a 24 meter stop, while Abysner barely does (1 minute). This is likely
> due to minor algorithmic differences in tissue loading precision.

|   | Depth | Duration | Runtime | Gas   | Mode         |
|---|-------|----------|---------|-------|--------------|
| ➘ | 60m   | 12min    | 12min   | 10/70 | CCR (SP 0.7) |
| ➙ | 60m   | 8min     | 20min   | 10/70 | CCR (SP 1.2) |
| ➚ | 24m   | 8min     | 28min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 21m   | 2min     | 30min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 18m   | 3min     | 33min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 15m   | 3min     | 36min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 12m   | 4min     | 40min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 9m    | 6min     | 46min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 6m    | 9min     | 55min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 3m    | 13min    | 68min   | 10/70 | CCR (SP 1.2) |
| ➚ | 0m    | 1min     | 69min   | 10/70 | CCR (SP 1.2) |
**CNS**: 29%  
**OTU**: 80  
*Subsurface (6.0.5576-CICD-release)*
</details>

<details>
<summary>DIVESOFT.APP</summary>

> **Observations:**
> DIVESOFT.APP does not display ascents between deco stops, and does not include the ascent time
> to the next stop in the total stop duration. This means the displayed stop time does not always
> match up to the runtime. Duration values below were derived by subtracting runtimes and the
> final ascent using the leftover runtime (which is displayed by DIVESOFT.APP).
>
> - DIVESOFT.APP produces a longer runtime (74 min vs 70 min for Abysner and 69 min for Subsurface).
> - DIVESOFT.APP displays 1.2 for the setpoint on the initial descent, while 0.7 was configured for
>   descents. It is unclear whether this is just a display choice (labeling the descent with the
>   bottom setpoint) or whether the low setpoint is not applied during descent.

|   | Depth | Duration | Runtime | Gas   | Mode         |
|---|-------|----------|---------|-------|--------------|
| ➘ | 60m   | 12min    | 12min   | 10/70 | CCR (SP 1.2) |
| ➙ | 60m   | 8min     | 20min   | 10/70 | CCR (SP 1.2) |
| ➚ | 24m   | 7min     | 27min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 24m   | 1min     | 28min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 21m   | 2min     | 30min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 18m   | 3min     | 33min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 15m   | 3min     | 36min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 12m   | 4min     | 40min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 9m    | 7min     | 47min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 6m    | 9min     | 56min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 3m    | 17min    | 73min   | 10/70 | CCR (SP 1.2) |
| ➚ | 0m    | 1min     | 74min   | 10/70 | CCR (SP 1.2) |
**CNS**: 31%  
**OTU**: 85  
*DIVESOFT.APP (Android 2.5.1)*
</details>


## Reference plan 9 (surface interval)
**30 meter, 30 minutes, repeated after 30-minute surface interval**

Both dives are identical: 30 meters for 30 minutes on air, with a 30-minute surface interval between
them. The second dive should produce noticeably longer decompression due to residual tissue loading
from the first dive.

| GF    | Salinity | Altitude | Last-deco stop | Surface interval |
|-------|----------|----------|----------------|------------------|
| 85/85 | Fresh    | 0 meters | 3 meter        | 30 minutes       |

### Dive 1

<details>
<summary>Abysner</summary>

|   | Depth | Duration | Runtime | Gas  |
|---|-------|----------|---------|------|
| ➘ | 30m   | 6min     | 6min    | 21/0 |
| ➙ | 30m   | 24min    | 30min   | 21/0 |
| ➚ | 3m    | 6min     | 36min   | 21/0 |
| ⏹ | 3m    | 8min     | 44min   | 21/0 |
| ➚ | 0m    | 1min     | 45min   | 21/0 |
**CNS**: 7%  
**OTU**: 20
</details>

<details>
<summary>Subsurface</summary>

|   | Depth | Duration | Runtime | Gas  |
|---|-------|----------|---------|------|
| ➘ | 30m   | 6min     | 6min    | 21/0 |
| ➙ | 30m   | 24min    | 30min   | 21/0 |
| ➚ | 3m    | 6min     | 36min   | 21/0 |
| ⏹ | 3m    | 8min     | 44min   | 21/0 |
| ➚ | 0m    | 1min     | 45min   | 21/0 |
**CNS**: 7%  
**OTU**: 19  
*Subsurface (6.0.5576-CICD-release)*
</details>

### Dive 2 (after 30-minute surface interval)

<details>
<summary>Abysner</summary>

|   | Depth | Duration | Runtime | Gas  |
|---|-------|----------|---------|------|
| ➘ | 30m   | 6min     | 6min    | 21/0 |
| ➙ | 30m   | 24min    | 30min   | 21/0 |
| ➚ | 6m    | 5min     | 35min   | 21/0 |
| ⏹ | 6m    | 1min     | 36min   | 21/0 |
| ➚ | 3m    | 1min     | 37min   | 21/0 |
| ⏹ | 3m    | 27min    | 64min   | 21/0 |
| ➚ | 0m    | 1min     | 65min   | 21/0 |
**CNS**: 7%  
**OTU**: 20
</details>

<details>
<summary>Subsurface</summary>

> **Observations:**
> Subsurface produces a 1 minute longer runtime (66 vs 65 minutes) with slightly different stop
> distributions. The stop structure is the same: both planners require a 6 meter stop on the
> repetitive dive that was not needed on the first dive.

|   | Depth | Duration | Runtime | Gas  |
|---|-------|----------|---------|------|
| ➘ | 30m   | 6min     | 6min    | 21/0 |
| ➙ | 30m   | 24min    | 30min   | 21/0 |
| ➚ | 6m    | 5min     | 35min   | 21/0 |
| ⏹ | 6m    | 2min     | 37min   | 21/0 |
| ⏹ | 3m    | 28min    | 65min   | 21/0 |
| ➚ | 0m    | 1min     | 66min   | 21/0 |
**CNS**: 12%  
**OTU**: 19  
*Subsurface (6.0.5576-CICD-release)*
</details>


