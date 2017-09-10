
Node.prototype.tag = function (tagName) {
    if (!tagName)
        return null;
    var t = document.createElement(tagName);
    this.appendChild(t);
    return t;
};

function $id(objectOrId) {
    return typeof objectOrId === "string" ? document.getElementById(objectOrId) : objectOrId;
}

Element.prototype.setText = function (text) {
    this.textContent = text;
    return this;
};

Element.prototype.attr = function (data) {
    if (!data || (typeof data !== "object"))
        return this;

    for (var name in data) {
        var val = data[name];
        if (typeof val === "function")
            continue;

        if (val === undefined)
            this.removeAttribute(name);
        else
            this.setAttribute(name, val);
    }
    return this;
};

//------------------------------------------------------------------------------

var locations = {};
var terminals = {};
var channels = {};
var groups = {};
var animations = {};
var swiches = {};
var rgbs = {};
var windows = {};
var trackBars = [];
var ws;

var currentTBar;
var currentWindow;

const state = {
    pirEnabled: null,
    scheduleEnabled: null
};

function updateState() {
    ws.send(JSON.stringify({
        action: "setState",
        state: state
    }));
}

function PIR(data) {
    this.power = data.power;
    this.state = data.state;
    this.value = data.value;

    var wnd = buildWindow("pir", "Oświetlenie zewnętrzne").children[1];

    new TrackBar(this.value, "pir", "Lampy").draw(wnd);


    const div = wnd.tag("div");
    div.style.display = "flex";

    var lbl = div.tag("label");
    lbl.style.flex = "auto";
    lbl.tag("input")
            .attr({type: "checkbox", checked: state.pirEnabled ? "checked" : undefined})
            .onclick = e => {
                state.pirEnabled = e.currentTarget.checked;
                updateState();
            };
    lbl.tag("span").setText("Czujniki PIR");


    lbl = div.tag("label");
    lbl.style.flex = "auto";
    lbl.tag("input")
            .attr({type: "checkbox", checked: state.scheduleEnabled ? "checked" : undefined})
            .onclick = e => {
                state.scheduleEnabled = e.currentTarget.checked;
                updateState();
            };
    lbl.tag("span").setText("Harmonogram");

    wnd.tag("hr");

    //--------------------------------------------------------------------------


    var d = data.schedule[data.currentDay - 1];
    if (d) {
        var tbl = wnd.tag("table").tag("tbody");
        var tr = tbl.tag("tr");
        tr.tag("td").setText("Dzień roku");
        tr.tag("td").setText(data.currentDay);

        tr = tbl.tag("tr");
        tr.tag("td").setText("Data");
        tr.tag("td").setText(d[0]);

        tr = tbl.tag("tr");
        tr.tag("td").setText("Strefa czasowa");
        tr.tag("td").setText("+" + d[1] + " UTC");

        tr = tbl.tag("tr");
        tr.tag("td").setText("Załączenie");
        tr.tag("td").setText(d[2]);

        tr = tbl.tag("tr");
        tr.tag("td").setText("Wyłączenie");
        tr.tag("td").setText(d[3]);

        tr = tbl.tag("tr");
        tr.tag("td").setText("Czas świecenia");
        tr.tag("td").setText(d[4]);

        tr = tbl.tag("tr");
        tr.tag("td").setText("Długość dnia");
        tr.tag("td").setText(d[5]);

        tr = tbl.tag("tr");
        tr.tag("td").setText("Wschód");
        tr.tag("td").setText(d[6]);

        tr = tbl.tag("tr");
        tr.tag("td").setText("Zachód");
        tr.tag("td").setText(d[7]);
    }

    //--------------------------------------------------------------------------

    tbl = wnd.tag("table");
    tbl.style.width = "100%";

    var tr = tbl.tag("thead").tag("tr");

    tr.tag("th").setText("Dzień");
    tr.tag("th").setText("Data");
    tr.tag("th").setText("+h UTC");
    tr.tag("th").setText("Załączenie");
    tr.tag("th").setText("Wyłączenie");
    tbl = tbl.tag("tbody");


    for (var i = 0; i < data.schedule.length; i++) {
        var sch = data.schedule[i];

        var tr = tbl.tag("tr");

        if (i === data.currentDay - 1)
            tr.style.fontWeight = "bold";

        tr.tag("td").setText(i + 1);

        for (var j = 0; j < 4; j++)
            tr.tag("td").setText(sch[j]);
    }


}

function Location(key, name) {
    this.key = key;
    this.name = name;
    this.terminals = [];
    this.channels = [];
    this.groups = [];
    this.switches = [];
    this.rgbs = [];
    locations[key] = this;

    var wnd = this.window = buildWindow("loc" + key, name);

    this.nav = document.createElement("div");
    this.nav.innerHTML = name;
    document.getElementById("nav-loc").appendChild(this.nav);

    this.nav.onclick = function () {
        wnd.setCurrent();
    };

    this.draw = function () {
        var parent = wnd.children[1];

        parent.tag("h4").setText("Grupy");
        for (var i = 0; i < this.groups.length; i++)
            this.groups[i].draw(parent, true);

        parent.tag("hr");
        parent.tag("h4").setText("RGB");
        for (var i = 0; i < this.rgbs.length; i++)
            this.rgbs[i].draw(parent);


        parent.tag("hr");
        parent.tag("h4").setText("Switche");
        for (var i = 0; i < this.switches.length; i++)
            this.switches[i].draw(parent, true);

        parent.tag("hr");
        parent.tag("h4").setText("Terminale");
        var ul = parent.tag("ul");
        for (var i = 0; i < this.terminals.length; i++)
            ul.tag("li").tag("a")
                    .setText(this.terminals[i].key)
                    .attr({href: "#term" + this.terminals[i].key});

        parent.tag("hr");
        parent.tag("h4").setText("Kanały");
        for (var i = 0; i < this.channels.length; i++)
            this.channels[i].draw(parent);
    };

}

function Group(key, data) {
    this.key = key;
    this.id = data.id;
    this.name = data.name;
    this.location = locations[data.loc];
    this.currentValue = data.cval;
    this.savedValue = data.sval;
    this.channels = [];
    this.switches = [];
    this.animations = [];
    groups[key] = this;

    if (this.location)
        this.location.groups.push(this);


    var wnd = this.window = buildWindow("term" + key, "Grupa " + this.location.name + " " + this.name);
    this.nav = document.createElement("div");
    this.nav.textContent = this.location.name + " " + this.name;
    this.nav.onclick = function () {
        wnd.setCurrent();
    };
    document.getElementById("nav-groups").appendChild(this.nav);

    for (var i = 0; i < data.chnls.length; i++)
        this.channels.push(channels[data.chnls[i]]);

    this.draw = function (parent, isLocation) {
        new TrackBar(this.currentValue, this.key,
                (!isLocation && this.location ? this.location.name + ": " : "") + this.name)
                .draw(parent);

        if (isLocation)
            return;

        if (this.animations.length) {
            parent.tag("hr");
            parent.tag("h4").setText("Animacje");

            for (var i = 0; i < this.animations.length; i++)
                this.animations[i].draw(parent, true);
        }

        parent.tag("hr");
        parent.tag("h4").setText("Kanały");
        for (var i = 0; i < this.channels.length; i++)
            this.channels[i].draw(wnd.children[1]);
    };
}

function Terminal(key, data) {
    this.channels = [];
    this.switches = [];
    this.location = locations[data.loc];
    this.key = key;
    this.id = data.id;
    this.revision = data.rev;
    terminals[key] = this;

    if (this.location)
        this.location.terminals.push(this);

    for (var name in data.chnls)
        new Channel(this, name, data.chnls[name]);

    var wnd = this.window = buildWindow("term" + key, "Terminal " + key);
    this.nav = document.createElement("div");
    this.nav.innerHTML = key;
    this.nav.onclick = function () {
        wnd.setCurrent();
    };
    document.getElementById("nav-terms").appendChild(this.nav);

    this.draw = function () {
        for (var i = 0; i < this.channels.length; i++)
            this.channels[i].draw(wnd.children[1]);
    };
}

function Channel(terminal, key, data) {
    this.terminal = terminal;
    this.key = key;
    this.id = data.id;
    this.location = locations[data.loc];
    this.currentValue = data.cval;
    this.savedValue = data.sval;
    this.trackBar = new TrackBar(this.currentValue, this.key, this.key);
    channels[key] = this;
    terminal.channels.push(this);
    if (this.location)
        this.location.channels.push(this);

    this.draw = function (parent) {
        this.trackBar.draw(parent);
    };
}

function Switch(key, data) {
    this.location = locations[data.loc];
    this.terminal = terminals[data.term];
    this.group = groups[data.group];
    this.upVal = data.up;
    this.downVal = data.down;
    this.name = data.name;
    this.key = key;
    var sw = this;

    swiches[key] = this;

    this.terminal.switches.push(this);
    if (this.location)
        this.location.switches.push(this);

    if (this.group)
        this.group.switches.push(this);

    this.draw = function (parent) {

        var span = parent.tag("span").attr({class: "switch-group"});

        var downTimeout = null;

        function addButton(up) {
            var btn = span.tag("button")
                    .setText(sw.name);

            btn.onmousedown = function () {
                ws.send(JSON.stringify({
                    action: "switchPress",
                    key: sw.key,
                    up: up
                }));

                downTimeout = setTimeout(btn.onmousedown, 30);
            };

            btn.onmouseout = btn.onmouseup = function () {
                clearTimeout(downTimeout);
            };
        }



        addButton(true);
        span.tag("br");
        addButton(false);

    };
}

function TrackBar(value, key, name) {

    this.key = key;
    this.name = name;
    this.rgbBar = false;
    var self = this;
    trackBars.push(this);

    var pos = Math.round(Math.sqrt(16 * value));

    this.draw = function (parent) {
        var tbar = parent.tag("div")
                .attr({class: "t_bar"});

        tbar.tag("span").setText(name);

        var tlabel = tbar.tag("span");

        function setPercent() {
            tlabel.setText(
                    (value < 40
                            ? (Math.round(10000 * value / 4096) / 100)
                            : (Math.round(1000 * value / 4096) / 10))
                    + "%")
                    .title = value;
        }

        setPercent();

        var ttrack = tbar.tag("input")
                .attr({
                    type: "range",
                    min: 0,
                    max: 256
                });

        if (this.rgbBar)
            ttrack.setAttribute("rgb", "rgb");

        tbar.ttrack = ttrack;
        ttrack.disabled = document.body.mobile;
        ttrack.value = pos;

        var tmarker = tbar.tag("div").attr({class: "marker"});
        tmarker.tag("div");


        this.setMarker = function (val) {
            tmarker.children[0].style.width = (100 * Math.sqrt(16 * val) / 256) + "%";
        };

        this.setMarker(value);

        ttrack.oninput = function (e) {
            value = Math.round(Math.pow(ttrack.value, 2) / 16);
            setPercent();
            //  self.setMarker(value);

            ws.send(JSON.stringify({
                action: "setPwm",
                channel: key,
                value: value,
                speed: 0
            }));
        };

        document.body.onclick = function () {
            if (currentTBar) {
                currentTBar.removeAttribute("active");
                currentTBar.ttrack.disabled = true;
            }
        };

        if (document.body.mobile)
            tbar.onclick = function (e) {
                if (currentTBar) {
                    currentTBar.removeAttribute("active");
                    currentTBar.ttrack.disabled = true;
                }

                currentTBar = tbar;
                ttrack.disabled = false;
                currentTBar.setAttribute("active", "active");
                e.cancelBubble = true;
            };

    };

}

function RGB(key, data) {
    this.location = locations[data.loc];
    this.key = key;

    this.valH = data.hv;
    this.valS = data.sv;
    this.valL = data.lv;

    this.tbH = new TrackBar(this.valH, data.hk, "Barwa");
    this.tbS = new TrackBar(this.valS, data.sk, "Nasycenie");
    this.tbL = new TrackBar(this.valL, data.lk, "Jasność");
    this.tbH.rgbBar = true;

    rgbs[key] = this;

    if (this.location)
        this.location.rgbs.push(this);

    this.draw = function (parent) {
        this.tbH.draw(parent);
        this.tbS.draw(parent);
        this.tbL.draw(parent);
    };
}


function Animation(key, data) {
    this.key = key;
    this.name = data.name;
    this.group = groups[data.grp];
    this.running = data.run;
    var anim = this;

    if (this.group)
        this.group.animations.push(this);
    /*
     
     var wnd = this.window = buildWindow("term" + key, "Grupa " + this.location.name + " " + this.name);
     this.nav = document.createElement("div");
     this.nav.textContent = this.location.name + " " + this.name;
     this.nav.onclick = function () {
     wnd.setCurrent();
     };
     document.getElementById("nav-groups").appendChild(this.nav);
     
     for (var i = 0; i < data.chnls.length; i++)
     this.channels.push(channels[data.chnls[i]]);
     */
    this.draw = function (parent, isLocation) {
        var btn = parent.tag("button").setText(this.name);

        btn.onclick = function () {
            ws.send(JSON.stringify({
                action: "animation",
                key: anim.key
            }));
        };

    };
}

addEventListener("load", function () {

    var host = location.hostname;

    //   if (host === 'localhost')
    //       host = "192.168.1.10";

    ws = new WebSocket("ws://" + host + ":8187/");

    ws.onopen = function () {
        ws.send(JSON.stringify({
            action: "getAll"
        }));

    };

    ws.onmessage = function (evt) {
        var data = JSON.parse(evt.data);
        // document.getElementById("result").innerHTML = JSON.stringify(data, null, 4);

        if (data.error) {
            alert("Błąd: " + data.error);
            return;
        }

        if (data.alert) {
            alert(data.alert);
            return;
        }


        switch (data.action) {
            case "ChannelValue":
                for (var name in data)
                    if (name !== "action") {

                        document.title = "evt: " + name + ": " + data[name];

                        for (var i = 0; i < trackBars.length; i++) {
                            if (trackBars[i].key === name) {
                                trackBars[i].setMarker(data[name]);
                            }

                        }
                    }

                break;
        }

        if (data.request && data.request.action && data.result)
            switch (data.request.action) {
                case "getAll":
                    loadData(data.result);
                    break;
            }
    };

    ws.onclose = function () {
        document.body.style.backgroundColor = "#eaa";
    };

});

function buildWindow(id, title) {
    var main = document.createElement("div");
    main.attr({
        id: id,
        class: "window"
    });

    var hdr = main.tag("div")
            .attr({class: "window-header"})
            .setText(title);

    if (document.body.mobile)
        hdr.onclick = function () {
            if (currentWindow) {
                currentWindow.parentNode.removeChild(currentWindow);
                currentWindow = null;
            }
            $id("menu").style.display = "flex";
        };

    main.tag("div")
            .attr({class: "window-content"});

    main.setCurrent = function () {
        window.location = "#" + main.id;
    };

    windows[id] = main;
    return main;
}

addEventListener("hashchange", function (e) {

    var wnd = windows[window.location.hash.substring(1)];
    if (!wnd)
        return;

    if (currentWindow)
        document.body.removeChild(currentWindow);

    if (document.body.mobile)
        $id("menu").style.display = "none";

    currentWindow = wnd;
    document.body.appendChild(wnd);

    localStorage.setItem("current-window", currentWindow.id);

});

function loadData(data) {
    for (var name in data.state)
        state[name] = data.state[name];

    for (var name in data.loc)
        new Location(name, data.loc[name]);

    for (var name in data.term)
        new Terminal(name, data.term[name]);

    for (var name in data.group)
        new Group(name, data.group[name]);

    for (var name in data.rgb)
        new RGB(name, data.rgb[name]);

    for (var name in data.switch)
        new Switch(name, data.switch[name]);

    for (var name in data.anims)
        new Animation(name, data.anims[name]);

    if (data.pir)
        var pir = new PIR(data.pir);

//------------------------------------

    var wndGroups = buildWindow("groups", "Grupy");

    for (var name in groups)
        groups[name].draw(groups[name].window.children[1]);

    for (var name in locations)
        locations[name].draw();

    for (var name in terminals)
        terminals[name].draw();

    var cWindow = localStorage.getItem("current-window");
    if (cWindow)
        window.location = "#" + cWindow;
    else
        wndGroups.setCurrent();



    var misc = buildWindow("misc", "Różne").children[1];

    function miscAction(key, name) {
        misc.tag("button")
                .setText(name)
                .onclick = function () {
                    ws.send(JSON.stringify({action: key}));
                };
        misc.tag("br");
    }

    for (var name in data.miscActions)
        miscAction(name, data.miscActions[name]);

    var event = document.createEvent("HTMLEvents");
    event.initEvent("hashchange", true, true);
    window.dispatchEvent(event);
}