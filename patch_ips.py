with open('./app/src/main/res/raw/ips_v4.txt', 'r') as f:
    lines = f.readlines()
    
# Make sure 223.27.177.0 is in it
found = any("223.27.177.0" in line for line in lines)
if not found:
    print("Not found, appending to file.")
    with open('./app/src/main/res/raw/ips_v4.txt', 'a') as f_out:
        f_out.write("223.27.176.0/24\n")
        f_out.write("223.27.177.0/24\n")
        f_out.write("216.154.223.0/24\n")
        
