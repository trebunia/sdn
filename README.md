DN

Aby odpalic mininet'a z nasza topologia nalezy wykonac komende jako root (albo z sudo):
```bash
mn --custom sdn.py --topo mytopo --controller=remote,ip=<controller ip>,port=6653
```

```controller ip``` to localhost jeśli mamy minineta tam gdzie floodlighta, albo adres hosta jeśli jesteśmy na wirtualce mininetowej.
