@ECHO OFF
SET Processor=ATtiny24
SET File=Release\HelperAttiny24.hex
SET AvrDude=avrdude.exe

"%AvrDude%" -e -P usb -c USBasp -p %Processor%  -U flash:w:"%file%":i

PAUSE
