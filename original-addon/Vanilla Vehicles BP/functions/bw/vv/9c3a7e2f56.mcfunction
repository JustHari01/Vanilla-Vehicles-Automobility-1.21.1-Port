## Initialisation
### Add objective

scoreboard objectives add world dummy
### Register to objective
scoreboard players add Initialised world 0

## The Commands
## wasd detection
execute if score Initialised world matches 0 run scoreboard objectives add fuel_warn_t dummy Fuel_Warning_Timer

## Mark as Initialised
scoreboard players set Initialised world 1