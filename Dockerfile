FROM lift-jungle-gym
RUN git clone git://github.com/hacklanta/lift-jungle-gym.git
RUN cd lift-jungle-gym && sbt container:start
