# SDN

Aby odpalic mininet'a z nasza topologia nalezy wykonac komende jako root (albo z sudo):
```bash
./sdn.py ControllerIP
```

```ControllerIP``` to localhost jeśli mamy minineta tam gdzie floodlighta, albo adres hosta jeśli jesteśmy na wirtualce mininetowej.

Po ukazaniu sie konsoli mininet mozna wygenerowac ruch poleceniem:
```bash
gentraffic 10
```
To polecenie spowoduje wygenerowanie 10 losowych przeplywow pomiedzy hostami a serwerami o roznej przepustowosci i o roznym czasie trwania.
