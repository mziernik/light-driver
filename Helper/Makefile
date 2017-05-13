# Plik wygenerowany przez JAvrMake

#Procesor
MMCU =		atmega328p
#Zegar
F_CPU =		16000000
#Optumalizacja
OPTIMIZE =	s
#Programowanie
AVRDUDE =	D:\Elektronika\Apps\AvrDude\AvrDude.exe -V -e -P usb -c USBasp -p $(MMCU) -U flash:w:"bin/$(TARGET).hex":i
#=========================================================================================
# Katalog źródeł
SRC =		src
# Katalog docelowy
DIST =		bin
# Nazwa pliku docelowego
TARGET =	dist
# Ścieżki toolchain-a
#TOOLCHAIN =	C:\Program Files (x86)\Atmel\Studio\7.0\toolchain\avr8\avr8-gnu-toolchain
TOOLCHAIN =	D:\Elektronika\Apps\avr8-gnu-toolchain
#LIBS =		C:\Program Files (x86)\Atmel\Studio\7.0\Packs\atmel\ATmega_DFP\1.0.90\gcc\dev\$(MMCU)
LIBS =		D:\Elektronika\Apps\avr8-gnu-toolchain\dfp\gcc\dev\$(MMCU)
#INCLUDE =	C:\Program Files (x86)\Atmel\Studio\7.0\Packs\atmel\ATmega_DFP\1.0.90\include
INCLUDE =	D:\Elektronika\Apps\avr8-gnu-toolchain\dfp\include
#------------------------------------------------------------------------------------------------
CC =		"$(TOOLCHAIN)\bin\avr-gcc.exe"
LD =		"$(TOOLCHAIN)\bin\avr-gcc.exe"
OBJCOPY =	"$(TOOLCHAIN)\bin\avr-objcopy.exe"
SHELL =		cmd.exe

CFLAGS = -x c \
	-funsigned-char \
	-funsigned-bitfields \
	-DNDEBUG \
	-DF_CPU=$(F_CPU)  \
	-I"$(INCLUDE)"  \
	-O$(OPTIMIZE) \
	-ffunction-sections \
	-fdata-sections \
	-fpack-struct \
	-fshort-enums \
	-Wall \
	-mmcu=$(MMCU) \
	-B "$(LIBS)" \
	-std=gnu99

LDFLAGS = -Wl,--start-group -Wl,-lm -Wl,--end-group -Wl,--gc-sections \
	-mmcu=$(MMCU) \
	-B "$(LIBS)" \
	-O$(OPTIMIZE) 

all: bin/$(TARGET).elf 	

#=======[ Sekcja generowana automatycznie. Nie należy jej modyfikować ]=========

DST = $(DIST)/main.o \
	$(DIST)/uart.o

$(DIST)/%.o: $(SRC)/%.c
	$(CC) $(CFLAGS) -c $< -o $@

#===============================================================================

bin/$(TARGET).elf: $(DST)
	@echo =========================================
	$(LD) $(LDFLAGS) -o $@ $(DST) 

	$(OBJCOPY) -O ihex -R .eeprom -R .fuse -R .lock -R .signature \
	-R .user_signatures  "bin/$(TARGET).elf" "bin/$(TARGET).hex"
	
	$(AVRDUDE)

clean:
	del /S /F /Q bin\\*

