rm -R world
cp -avr map world
screen -S sw1 -X quit
screen -S sw1 -m -d java -Xincgc -server -jar -Xms600m -Xmx600m spigot-1.16.2.jar
