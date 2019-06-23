package com.mihael.bots.fighter

import  ...

class Fighter : MakBot(), SkillListener {

    private val webServices = OCCodeWebServices("my_token", this)
    private var currentFood = "Lobster"

    override fun onStart(vararg arguments: String?) {
        super.onStart(*arguments)
        webServices.apply {
            setup {
                setIsSafeToStop { !Robot.isInCombat() }
                
                addCustomAction("Food", "Select", listOf("Lobster", "Salmon", "Orange")) { 
                  currentFood = it
                  println("User selected $it to be used as current food.");
                };
                
                // Best practice is to call update in setup.
                update {
                    setCustomMetric("Current food", currentFood) // Displayed in metrics table.
                    setCustomMetric("Health", "${Health.getCurrentPercent()}%") // Displayed in metrics table.
                    setCustomMetric("Experience", experience) // Displayed in metrics table AND graph.
                    setCustomMetric("Experience p/h", (experience * 3600000.0) / runtime)// Displayed in metrics table AND graph.
                    setCustomMetric("Kills", productAmount)// Displayed in metrics table AND graph.
                    setCustomMetric("Kills p/h", (productAmount * 3600000.0) / runtime)// Displayed in metrics table AND graph.

                    setStatus(status)
                    setLoginUsername(Environment.getAccountAlias())
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        webServices.onPause() // Call this on bot pause so it syncs up with the server.
    }

    override fun onLevelUp(event: SkillEvent?) {
        super.onLevelUp(event)
        event?.apply { 
          webServices.sendNotification("Level up!", "$skill is now ${skill.currentLevel}!", OCCodeWebServices.Notification.INFORMATION); 
        }
    }

    override fun onResume() {
        super.onResume()
        webServices.onResume() // Call this on bot resume so it syncs up with the server.
    }
}
