# DeepMutation
DeepMutation is a mutation tool that learns how to generate mutants that are 
similar to *real bugs*.

At its core, DeepMutation relies on a Neural Machine Translation model that was
trained to translate fixed code into buggy code. DeepMutation has learned from
thousands of real world bug-fixes performed by developers, how to generate 
mutants that resemble real buggy code.

## Table of Contents
1. [Installation](#install)  
  a. [Setting up the environment](#setup)  
  b. [Building](#build)  
2. [Running](#run)
3. [How it works](#how)
4. [Credits](#credits)
5. [Paper References](#refs)

<a name="install"></a>
## Installation

<a name="setup"></a>
### Setting up the environment
DeepMutation uses [seq2seq](https://github.com/google/seq2seq.git), a 
Tensorflow framework.

In our study, we worked with tensorflow 1.3 and seq2seq 0.1.  
We include the script [setup_env.sh](setup_env.sh) to guide the installation process.

```
$ ./setup_env.sh
```

<a name="build"></a>
### Building
DeepMutation can be compiled into lib/DeepMutation.jar with:
```
$ ant dist
```

<a name="run"></a>
## Running
DeepMutation runs using a configuration file:
```
$ java -jar DeepMutation.jar config_file
```
An [example](config) is provided.

This will generate and test mutants.

<a name="how"></a>
## How it works
TODO Michele will describe the high-levle idea of the research paper.

<a name="credits"></a>
## Credits
The infrastructure of DeepMutation was created by Jason Kimko ([@jskimko](https://github.com/jskimko)) and Shiya Wang ([@sywang15](https://github.com/sywang15)).
The underlying model was created and trained by Michele Tufano and Cody Watson, based on seq2seq.

<a name="refs"></a>
## Papers References
TODO ICSME + ICSE tool demo submission
