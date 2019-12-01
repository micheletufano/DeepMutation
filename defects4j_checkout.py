#!/usr/bin/env python3

projects = {
            'Chart': 1,
            'Lang': 1,
            #'Chart': 26,
            #'Closure': 176,
            #'Lang': 65,
            #'Math': 106,
            #'Mockito': 38,
            #'Time': 27,
           }

# DO NOT PASS.

import sys, os, subprocess, shutil

defects4j = 'defects4j'
fail_ok = False
user_choices = []
failed = {name : [] for name in projects}

def exit(rc):
    global failed
    if any(failed.values()):
        print('Failed checkouts:')
        for name, revs in failed.items():
            if not revs: 
                continue

            print(f'  {name}', end=' ')
            for rev in revs:
                print(f'{rev[0]}', end=' ')
            print()

            user = input(f'\n>> Remove revision? [Y/n] ')
            if user == '' or user.lower() == 'y':
                for rev in revs:
                    shutil.rmtree(rev[1])

    sys.exit(rc)

def usage():
    print(f'usage: {sys.argv[0]} [-f] proj_name...')

if not shutil.which(defects4j):
    print(f"error: cannot find executable '{defects4j}'")
    exit(1)

if len(sys.argv) == 1:
    usage()
    exit(1)

for i in range(1, len(sys.argv)):
    arg = sys.argv[i]
    if arg == '-f':
        fail_ok = True
    elif arg == '-h':
        usage()
        exit(0)
    else:
        if arg not in projects:
            print(f"error: bad project name '{arg}'")
            exit(1)
        else:
            user_choices.append(arg)

if not user_choices:
    usage()
    exit(1)

for key in list(projects.keys()):
    if key not in user_choices:
        projects.pop(key)

base = os.path.join('data', 'in')
os.makedirs(base, exist_ok=True)

for name, count in projects.items():
    for num in range(1, count+1):
        for ver in ['b', 'f']:
            num = str(num)
            rev = num + ver
            d = os.path.join(base, name, rev)

            os.makedirs(d, exist_ok=True)

            cmd = [defects4j,
                   'checkout',
                   '-p', name,
                   '-v', rev,
                   '-w', d, ]

            try:
                subprocess.check_call(cmd)
            except subprocess.CalledProcessError:
                failed[name].append((rev, d))

                if not fail_ok:
                    user = input(f'\n\n>> Failed to checkout {name} {rev}. Continue? [Y/n] ')
                    if user == '' or user.lower() == 'y':
                        pass
                    else:
                        exit(1)
            print()

exit(0)
