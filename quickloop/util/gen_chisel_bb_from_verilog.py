import re
def convert(verilog_file):
    state = 0
    with open(verilog_file, 'r') as v:
        print('import chisel3._')
        print('import chisel3.experimental._\n')

        param_widths = []

        for line in v:
            tokens = line.split()
            if len(tokens) > 0:
                if '//' in tokens[0]:
                    print(line, end='')
                    continue
                if state == 0:
                    module_name = tokens[0]
                    print(f'class {module_name} extends ExtModule(', end='')
                    if '#' in line:
                        print('Map(')
                        state = 1
                    else:
                        print('){')
                        state = 2
                elif state == 1:
                    if tokens[0] == ')':
                        print(')){')
                        state = 2
                    else:
                        param_name = re.findall(r'\.(.*)\(', line)
                        param_default = re.findall(r'\((.*)\)', line)
                        param_digital = 'DECIMAL' in line
                        param_string = 'String' in line

                        if len(param_name) > 0:
                            print(f"""\t"{param_name[0]}" -> {param_default[0]}, """)

                else:
                    port_name = re.findall(r'\.(.*)\(', line)
                    port_width = re.findall(r'// (.*)-bit', line)
                    port_direction = re.findall(r'-bit (.*):', line)
                    port_comment = re.findall(r'//.*', line)

                    if len(port_name) > 0:
                        if not port_width[0].isnumeric():
                            param_widths.extend(port_width)
                        print(f'\tval {port_name[0]} = IO({port_direction[0].capitalize()}(UInt({port_width[0]}.W))) \t{port_comment[0]}')

                    if ');' in line:
                        print()
                        for par in param_widths:
                            print(f"""\tdef {par} = params("{par}").asInstanceOf[IntParam].value.toInt""")
                        print('}')

convert('/work/tayyeb/PycharmProjects/fast/fast/util/template.v')

