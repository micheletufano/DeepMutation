#!/usr/bin/env python3

projects = {
            'Chart': 26,
            'Closure': 176,
            'Lang': 65,
            'Math': 106,
            'Mockito': 38,
            'Time': 27,
           }

# DO NOT PASS.

import sys, os, subprocess, shutil

defects4j = 'defects4j'
fail_ok = False
user_filter = []
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
                print(f'{rev}', end=' ')
            print()
    elif rc == 0:
        print('Success.')
    sys.exit(rc)

if not shutil.which(defects4j):
    print(f"error: cannot find executable '{defects4j}'")
    exit(1)

for i in range(1, len(sys.argv)):
    arg = sys.argv[i]
    if arg == '-f':
        fail_ok = True
    elif arg == '-h':
        print(f'usage: {sys.argv[0]} [-f]')
        exit(0)
    else:
        if arg not in projects:
            print(f"error: bad project name '{arg}'")
            exit(1)
        else:
            user_filter.append(arg)

if user_filter:
    for key in list(projects.keys()):
        if key not in user_filter:
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
                failed[name].append(rev)

                if not fail_ok:
                    user = input(f'\n\n>> Failed to checkout {name} {rev}. Continue? [Y/n] ')
                    if user == '' or user.lower() == 'y':
                        pass
                    else:
                        exit(1)
            print()

exit(0)
