#-------------- MCS Generation ----------------------
set_property BITSTREAM.CONFIG.EXTMASTERCCLK_EN div-1  [current_design]
set_property BITSTREAM.GENERAL.COMPRESS TRUE          [current_design]
set_property BITSTREAM.CONFIG.UNUSEDPIN Pullnone      [current_design]
set_property CFGBVS GND                               [current_design]
set_property CONFIG_VOLTAGE 1.8                       [current_design]

