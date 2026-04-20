[![Build](https://github.com/NeoTech-Software/abysner/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/NeoTech-Software/abysner/actions/workflows/build.yml)
[![Coverage (core)](https://img.shields.io/codecov/c/github/NeoTech-Software/abysner/main?flag=domain&label=coverage%20(core))](https://app.codecov.io/gh/NeoTech-Software/abysner/flags)
[![Coverage (ui)](https://img.shields.io/codecov/c/github/NeoTech-Software/abysner/main?flag=presentation&label=coverage%20(ui))](https://app.codecov.io/gh/NeoTech-Software/abysner/flags)

![Abysner - The open-source mobile dive planner](resources/readme-header.png)
  
[![Get it on Google Play](resources/store-badge-google.svg)](https://play.google.com/store/apps/details?id=nl.neotech.app.abysner)
[![Download on the App Store](resources/store-badge-apple.svg)](https://apps.apple.com/nl/app/abysner/id6636477320)

**The decompression models we use and trust today to plan our dives are the result of decades of collective research by thousands of people. There is a lot of software available to plan dives, built on top of this research. However on Android and iOS the options are limited: either expensive and proprietary, or lacking a good mobile-friendly interface.**

Abysner was built with the goal of giving something back to the diving community. It is open-source, built with Kotlin Multiplatform and Compose Multiplatform (the best cross-platform mobile solution to date), available on both Android and iOS, and free to inspect and verify.

> **Disclaimer:** 
> Diving is a potentially dangerous activity. Do not use this application without proper training in diving and decompression techniques. This application is in an early development stage, and we cannot guarantee that it is free of bugs. Always cross-validate any information presented by the application with reliable sources.
>
> No one associated with this project (including authors, contributors, advisors, or any other affiliates) can be held responsible for the outcomes of your use of the information provided by this application. The use of this application is entirely at your own risk.


# Philosophy
Abysner is built with simplicity in mind. Other planners may offer more data or options, but Abysner
is designed to be easy and quick to use, correct enough to trust, on the device you already have in
your pocket.

Prioritizing usability over complexity makes it a practical tool in the field, but as an instructor
myself, also a good fit for the classroom: you can walk students through a real plan on a phone,
no laptop required.


# Features
**Abysner is under active development (imperial units are not available yet), but it already
supports:**

- **Full open-circuit (OC) dive planning**
- **Full closed-circuit rebreather (CCR) dive planning** with configurable setpoints and bailout
- **Buhlmann ZHL-16 A, B and C** with gradient factors
- **Multi-gas:** Air, Nitrox, Oxygen, Trimix, Helitrox, Heliox
- **Intuitive gas selector** showing MOD based on oxygen and gas density
- **User configurable**:
    - SAC rates
    - Environment (salinity, altitude)
    - Descent/ascent rates
    - Gradient factors
    - Deco stop intervals and last deco stop
    - Max PPO2 and gas switch time
    - CCR setpoints (low and high) with automatic switch depth
- **Dive profile graph** with average depth and ceiling
- **Dive plan:** runtime, depth, duration, gas, gas switches, ascents, descents, and time to deco (NDL)
- **Contingency plan:** Automatic longer and deeper contingency plan with configurable time and depth
- **Gas plan:** used and reserve/bailout gas calculation, per-cylinder requirements, warnings, density and PPO2 information
- **Multi-level** dive planning
- **Multi-dive** planning with surface intervals


# Dive planning
Dives are planned based on bottom sections with automatically calculated ascents and descents.

- To reach the depth of a section the descent or ascent time that is required to reach this depth is
  subtracted from that section's bottom-time.
- The final ascent to the surface is not part of any section and the time it takes is not
  subtracted from anything, instead added to the total dive so far.

**Example (assume no deco):**  
A planned section at 30 meters for 20 minutes at 5 m/min descent/ascent speed will turn into a dive
profile where the first 6 minutes is a descent, then 14 minutes bottom-time, then 6 minutes ascent
time. The total dive time this makes is 26 minutes.


# Gas planning
When the app calculates a dive profile for decompression, it also calculates how much gas is
required for the dive. The gas plan shows two numbers per cylinder: **used gas** and **reserve gas**
for open-circuit dives, or **loop gas** and **bailout gas** for closed-circuit dives.

Both numbers are always based on the contingency (Deeper & Longer) profile, not the base plan.
This means the contingency time and depth settings directly affect gas requirements.

- **Used gas:** How much gas one diver needs to normally complete the profile.

- **Reserve gas:** How much extra gas is required to safely bring up an out-of-air diver from the
worst-possible point during the dive. It is calculated based on the worst TTS in terms of gas usage
(See: [FAQ No. 5](#faq)), however during an out-of-gas scenario your buddy may have a completely
different SAC rate than normal (a panic rate). To account for this, reserve gas is calculated using
the emergency SAC rate, usually at least 2 times higher than your normal SAC rate.

For CCR dives, gas planning works differently. There is no reserve gas for an out-of-air buddy.
Instead, the gas plan shows **loop gas** (diluent and oxygen consumed in the closed-loop) and
**bailout gas**. Bailout tells you how much open-circuit gas you need if the loop fails at the
worst-possible point. It is calculated at the normal SAC rate. CCR divers can adjust their SAC rate
setting to account for the stress of a bailout scenario as they see fit.


# Compared to other planners
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
**20 meter, 20 minutes\*, single-gas (21/0)**

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
**30 meter, 30 minutes\*, multi-gas**

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
**OTU**: 35
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
**45 meter, 15 minutes\*, multi-gas, trimix**

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
| ⏹ | 3m    | 5min     | 31min   | 50/0  |
| ➚ | 0m    | 1min     | 32min   | 50/0  |
**CNS**: 9%  
**OTU**: 25
</details>

<details>
<summary>Subsurface</summary>

> **Observations:**
> Subsurface merges the ascent from 21 meter to 9 meter into the 9 meter stop, while Abysner shows
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
>   to one minute Abysner produces total runtime of 32min, compared to 33min for DIVESOFT.APP. The
>   individual stop distributions differ ever so slightly.

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
**60 meter, 20 minutes\*, multi-gas, trimix, altitude**

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
| ⏹ | 6m    | 6min     | 44min   | 50/0  |
| ⏹ | 3m    | 12min    | 56min   | 50/0  |
| ➚ | 0m    | 1min     | 57min   | 50/0  |
**CNS**: 15%  
**OTU**: 40
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
| ⏹ | 6m    | 6min     | 48min   | 21/20 |
| ⏹ | 3m    | 15min    | 63min   | 21/20 |
| ➚ | 0m    | 1min     | 64min   | 21/20 |
**CNS**: 9%  
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
| ➚ | 6m    | 5min     | 35min   | 21/0 | CCR (SP 1.2)  |
| ⏹ | 6m    | 1min     | 36min   | 21/0 | CCR (SP 1.2)  |
| ⏹ | 3m    | 2min     | 38min   | 21/0 | CCR (SP 1.2)  |
| ➚ | 0m    | 1min     | 39min   | 21/0 | CCR (SP 1.2)  |
**CNS**: 17%  
**OTU**: 47
</details>

<details>
<summary>Subsurface</summary>

> **Observations:**
> Subsurface does not require a 6 meter stop, while Abysner barely does (1 minute). This is likely
> due to minor algorithmic differences in tissue loading precision.

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
| ⏹ | 6m    | 4min     | 41min   | 21/0 | OC            |
| ⏹ | 3m    | 8min     | 49min   | 21/0 | OC            |
| ➚ | 0m    | 1min     | 50min   | 21/0 | OC            |
**CNS**: 14%  
**OTU**: 38
</details>

<details>
<summary>Subsurface</summary>

> **Observations:**
> Subsurface produces a 1 minute longer runtime (51 vs 50 minutes), which is typical algorithmic
> variance also seen in the other reference plans. This may be triggered by the slower initial
> ascent in Abysner (5 vs 4 minutes) allows for more off-gassing during the ascent itself, resulting
> in slightly shorter subsequent stops.

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
> - DIVESOFT.APP produces a significantly longer runtime (60 min vs 50/51 min for Abysner and
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
**60 meter, 20 minutes\*, CCR with 10/70 trimix diluent, setpoints 0.7 low / 1.2 high**

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
| ⏹ | 12m   | 5min     | 41min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 9m    | 6min     | 47min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 6m    | 8min     | 55min   | 10/70 | CCR (SP 1.2) |
| ⏹ | 3m    | 13min    | 68min   | 10/70 | CCR (SP 1.2) |
| ➚ | 0m    | 1min     | 69min   | 10/70 | CCR (SP 1.2) |
**CNS**: 29%  
**OTU**: 81
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
> - DIVESOFT.APP produces a longer runtime (73 min vs 69 min for Abysner and Subsurface).
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


# FAQ

<details>
<summary><strong>1. Does Abysner round to minutes?</strong></summary>

Yes and no, Abysner currently calculates dive plans in whole minutes. The reasoning behind this is
that most of the time we are interested as divers in minutes only, we generate plans to write
down on our wetnotes and for simplicity reasons we do that in minutes.

The above has led me to believe that doing the planning in seconds first, then rounding those to
minutes is kinda pointless and leads to less accurate plans.

> *Example:* if an ascent to a certain level takes 4:20 minutes. This will be rounded to either 4
or 5 minutes but the tissue loading internally was based on those 4 minutes and 20 seconds. So when
following the plan on paper, you either decompress too little during the ascent hitting the ceiling,
or you go a bit slower compared to what was calculated and potentially on-gas certain slower
compartments a bit more.

Instead, Abysner calculates from the very start in whole minutes so that the eventual dive plan does
not need rounding. The downside of this technique is that we have to round some other things, like
ascent and descent speeds. However since the eventual dive plan will be in minutes anyway, this
will be the more realistic case.

*TLDR:* Divers usually require plans in whole minutes, thus calculate in minutes.

Do I consider adding second precision in the future, will it be a setting? Not sure, but the answer
may very well be yes.
</details>

<details>
<summary><strong>2. Why does Abysner give different plans compared to tool X?</strong></summary>

There are countless reasons why this app may give you a different plan, first and foremost: there
is not a single definition of what a dive planner is and how it should work, and this is even
true for the Bühlmann model. There are just many small undefined details that are up to the
implementation to decide.

Robert Helling explains the differences between planners very well in his blog post "Why is Bühlmann
not like Bühlmann":
https://thetheoreticaldiver.org/wordpress/index.php/2017/11/02/why-is-buhlmann-not-like-buhlmann/

If you do feel like something is a bug, feel free to report an issue.
</details>

<details>
<summary><strong>3. Are you qualified to write this software?</strong></summary>

**No.**

I'm a recreational and semi-closed rebreather instructor, technical diver, and consider myself to be
a professional software engineer and awesome programmer. But I'm not a scientist, mathematician,
doctor, or anything like that, so the answer is no.
</details>

<details>
<summary><strong>4. Is Abysner free?</strong></summary>

Abysner is open-source, licensed under AGPLv3. You are free to use, build, modify, and redistribute
it under the terms of that license (that is the "free" part).

The official builds distributed through the Play Store and App Store do carry a small fee. This
covers Apple's yearly developer fee and helps offset other project costs such as a domain name,
development tools, and hardware to test on. If you'd rather not pay, you are welcome to build it
yourself from source or obtain a build from someone else. The license explicitly allows this.
</details>


<details>
<summary><strong>5. How does Abysner determine the worst-case point for reserve gas?</strong></summary>

**TLDR: The TTS that consumes the most gas.**

Most people will tell you this is a point during the deepest part of the dive, or more precisely at
the end of the planned bottom time (just before final ascent). However, with multi-level dive
profiles this can be a slightly more complex story. Because where does the final ascent begin? At
the end of the last bottom section? What if the deeper portion of the dive is at the beginning, and
super short? While a slightly shallower but longer section causes actual decompression time?

>**Example:**
> Assume an ascent/descent rate of 10 meter per minute (40/80 gf). Take a profile where you start
> your dive by descending to 40 meters, then have 1 minute of bottom time after which you ascend to
> 30 meters and stay there for 20 minutes. Then the deepest part of your dive is not the
> worst-possible point to ascend anymore, since your deco obligation at the 40 meters part is still
> essentially non-existent. However, at the end of the 30 meters section you have about 6 minutes of
> deco to complete (with a 50% mix). So those are 6 extra minutes of gas usage, compared to
> ascending from 40 meters, yes the gas usage is shallower, but there is more time to breathe more
> gas as well. So is the deepest section still the worst?

To correctly calculate the worst-possible ascent point during a dive Abysner uses TTS, also known as
time-to-surface. Essentially it calculates how long it would take from any given point during the dive
to ascend safely to the surface (including deco stops if required). The point during the dive where
the TTS is the highest is most likely the worst-possible ascent point. However even this is not the
complete story (see example), in some cases a shallower section at the end of the dive may cause a
longer TTS, and thus potentially more gas usage. So the app calculates ascent gas usage for multiple
TTS points, then takes the maximum numbers for each mix. These maximum numbers are the basis for
calculating the reserve gas requirements.
</details>


# Contributing
If you'd like to contribute, please open an issue or start a discussion before putting significant
effort into a feature or refactor. This project has a specific scope and direction, and not all
pull requests will be accepted. A conversation upfront is the best way to make sure your time is
well spent. Detailed contribution guidelines are not yet available, but the basics are covered here.

All contributors are required to sign a [Contributor License Agreement (CLA)](cla.txt) before
their pull request can be merged. The CLA process is automated via
[CLA Assistant](https://github.com/contributor-assistant/github-action) and will prompt you
automatically when you open a pull request.

> **A personal note on AI:** I use AI assistance where it makes sense, writing boilerplate,
> surfacing bugs, and exploring ideas. But it can't replace correctness and safety, since AI is
> probabilistic, and a safety-relevant application like a dive planner must be deterministic.
> 
> Abysner is not a vibe-coded project. Every architectural decision is made deliberately, every
> algorithm and its results are validated against [known references](#compared-to-other-planners)
> and diving organization standards, peer-reviewed by subject-matter experts including active
> technical diving instructors, and every line of code is reviewed by a person. AI tools are, in my
> opinion, a means to better quality through efficiency, not a substitute for careful and deliberate
> development.

# Credits
This project builds on a lot of prior work: decompression research, open-source planning software,
and the broader diving community. Credit where credit is due.

<details>
<summary>Details</summary>

- [The Theoretical Diver Blog](https://thetheoreticaldiver.org):
    - Particularly helpful for understanding gradient factors, which are not as straightforward to
      implement as the theory makes you believe.
    - Also, a useful reference for the Schreiner equations used in CCR planning.
- Erik C. Baker's publications:
    - Understanding M-Values
    - Clearing Up The Confusion About "Deep Stops"
    - Oxygen Toxicity Calculations
- Open-source software used for validation, comparison, and inspiration (in no particular order):
  [GasPlanner](https://github.com/jirkapok/GasPlanner), [nyxtom/dive](https://github.com/nyxtom/dive), [Subsurface](https://github.com/subsurface/subsurface), [DecoTengu](https://wrobell.dcmod.org/decotengu/index.html)
- [ScubaBoard](https://scubaboard.com): a genuinely valuable source of diving discussions and technical knowledge. For
  example [this thread on the Schreiner equations for CCR](https://scubaboard.com/community/threads/schreiner-equations-for-ccr.554316).

</details>
