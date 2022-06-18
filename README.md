# RiskyOrdealAgent
Risk-solver agent developed for Strategy Game Programming @ TU Wien

## How to run
1. Download the environment from Professor Lukas Grassauer, _grassauer-pss-risk-env-v3_.
2. Download the _RiskyOrdealAgent.jar_ into the _grassauer-pss-risk-env-v3/agents/_ folder.
3. Open the command line inside the _grassauer-pss-risk-env-v3/_ folder and run `java -jar sge-1.0.2-exe.jar match --file=sge-risk-1.0.2-exe.jar --directory=agents -a RiskyOrdealAgent <competitor_agent> -c <computation_time>`.
   - For example: `java -jar sge-1.0.2-exe.jar match --file=sge-risk-1.0.2-exe.jar --directory=agents -a RiskyOrdealAgent AlphaBetaAgent -c 1`.

## Developers
- Henrique Pereira, 12137592
- Kirill Medovshchikov, 12144024