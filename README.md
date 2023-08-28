# Thermopylae
## Named after a famous clipper ship, this tool aims to leverage an LLM to aid a user in case of a possible cyber attack.
## Intended audience
Both individual users and (corporate) system admins.
### I am a user.
There will be a release that will contain a jar file. download that and double click the download, it should help you in a step-by-step manner
### I am an admin.
Download the project and study it. You might not be familiar with Java but it should be readable enough. Devise some way to have it pre-installed for users to activate. You might want to point it to an on-premise endpoint for the LLM.
Note that the project is MIT license, feel free to adapt as required.
## Design considerations.
Some unusual choices were made as a result of this being a very specific targeted tool.
### Linear code
To help admins and (security) auditors, the code is as linear as I could make it. Boring but easy reading.
### Platform
The code level is Java7 but any available JRE should run it fine. An admin might need to adjust pom.xml and rebuild using Maven, for instance if only Java 1.4 is available as runtime.
### Why Java?
Java at this level should be available everywhere and should be platform independant "out-of-the-box". It also offers the most minimal attack vector for attempts to use this tool itself for attacks. Boring but solid.
## What does it do?
TL;DR The user should have a chat session open with for instance ChatGPT. The user starts the tool and it will guide both the LLM and the user in a step-by-step process as a first-responder in case of a cyber attack. The intent is that the LLM will be able to adapt to whatever it finds. The system clipboard will be used to copy commands and output between the tool and the LLM under the user's control.
## ChatGPT-4's own opinion
> That sounds like a highly valuable application of language models like this one. Leveraging the LLM's knowledge base for immediate problem-solving can be a game-changer, especially in remote work scenarios where immediate human expert assistance might not be readily available. Your approach of combining automated commands with real-time user guidance could make the tool both powerful and accessible, bridging the gap between expert knowledge and user action. It's a compelling use case for sure.
