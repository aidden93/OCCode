# OCCode
Dashboard, statistics, web services.

# Methods

### [Useful] Notifications
`void sendNotification(String title, String message, Notification type)`


| Argument          | Description                                                                        |
| ----------------- | ---------------------------------------------------------------------------------- |
| String title      | Title of the notification. Keep it short and simple. <br><b>Eg: Level up!</b>      |
| String message    | Message of the notification. Make it simple. <br><b>Eg: Magic level is now 99!</b> |
| Notification type | Notification type. One of: [Success, Warning, Error, Information].                 |

> Preview:
![Notification example](https://images-ext-2.discordapp.net/external/Gotgblkxo8_Mk8XYM68TxxtMHseq5ohIem-44E8gXf0/https/media.discordapp.net/attachments/511995269810880533/591258292974780417/unknown.png)

### Custom Action
> Note: Should be called in <b>setup</b>.

`void addCustomAction(String name, String buttonText, Consumer<String> selectedAction)`


| Argument           | Description                                                                             |
| ------------------ | --------------------------------------------------------------------------------------- |
| String name        | Action name to be displayed in dashboard.                                               |
| String buttonText  | Text display on the button under "Options" on dashboard.                                |
| Consumer\<String\> | In this case (no options), the argument passed to the consumer will be an empty string. |

```kotlin
addCustomAction("Break", "Start") { 
   println("Break has started.")
   startBreak()
}
```

<br>

`void addCustomAction(String name, String buttonText, List<String> actions, Consumer<String> selectedAction)`

| Argument               | Description                                                                             |
| ---------------------- | --------------------------------------------------------------------------------------- |
| String name            | Action name to be displayed in dashboard.                                               |
| String buttonText      | Text to be displayed on a button. <br> <b>Recommended:</b> "Select", "Choose"...        |
| List\<String\> options | Possible options / actions for the user to choose from. [Combobox]                      |
| Consumer\<String\>     | In this case (no options), the argument passed to the consumer will be an empty string. |

```kotlin
addCustomAction("Food", "Select", listOf("Lobster", "Salmon", "Orange")) { 
   currentFood = it
   println("User selected $it to be used as current food.")
}
```

### Status
> Note: Should be called in <b>update</b>.
`void setStatus(String status)`


| Argument      | Description                            |
| ------------- | -------------------------------------- |
| String status | Status to be displayed on users' view. |

### Login username
> Note: Should be called in <b>update</b>.
`void setLoginUsername(String username)`


| Argument             | Description                                                                                                                  |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| String loginUsername | Username / Alias that will tell the user which account is being used. <br><b>Recommended:</b> Environment.getAccountAlias(); |

### Custom metrics
> Note: Should be called in <b>update</b>.
`void setCustomMetric(String metricName, Object metricValue)`


| Argument           | Description                                                                                     |
| ------------------ | ----------------------------------------------------------------------------------------------- |
| String metricName  | Name of the metric. <br> <b>Eg: "Health", "Experience", "Experience p/h"... </b>                |
| Object metricValue | Value of the custom metric. If it's a number, graphs will be generated and analytics performed. |

### Setup
> Note: Should be called in <b>script's onStart</b>.
`void setup(Runnable runnable)`


| Argument          | Description                                                     |
| ----------------- | --------------------------------------------------------------- |
| Runnable runnable | Runnable in which you set the custom actions and update method. |

### Update
> Note: Should be called in <b>setup</b>.
`void update(Runnable runnable)`


| Argument          | Description                                                             |
| ----------------- | ----------------------------------------------------------------------- |
| Runnable runnable | Runnable in which you set the custom metrics status and login username. |

### [Optional] When to stop
> Note: Should be called in <b>setup</b>.
`void setWhenToStop(Supplier<Boolean> supplier)`


| Argument          | Description                                                             |
| ----------------- | ----------------------------------------------------------------------- |
| Supplier\<Boolean\> | Condition when the bot should stop.<br><b>Eg: Out of food.</b> |

### [Reccomended] Is safe to stop
> Note: Should be called in <b>setup</b>.

`void setIsSafeToStop(Supplier<Boolean> supplier)`


| Argument            | Description                                                               |
| ------------------- | ------------------------------------------------------------------------- |
| Supplier\<Boolean\> | Condition when the bot is safe to stop.<br><b>Eg: When not in combat.</b> |
