FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates curl fontconfig fonts-dejavu-core libgl1-mesa-dri libglx-mesa0 libopenal1 libxrandr2 libxcursor1 \
        libxxf86vm1 libxi6 libxtst6 python3 xvfb \
    && rm -rf /var/lib/apt/lists/*
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends x11-xserver-utils \
    && rm -rf /var/lib/apt/lists/*

ENV LIBGL_ALWAYS_SOFTWARE=1 LP_NUM_THREADS=4 ALSOFT_DRIVERS=null ALSOFT_LOGLEVEL=3
