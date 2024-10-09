// Process
sig Process {
	name: lone String,
	flowNodes: set FlowNode
	subProcesses: set SubProcess
}

// Flow node
abstract sig FlowNode {
	// sequence flows
	outgoingSequenceFlows: set SequenceFlow,
	incomingSequenceFlows: set SequenceFlow,
}

// Special flow nodes.
sig Activity extends FlowNode {
}

sig StartEvent extends FlowNode {
}

sig EndEvent extends FlowNode {
}

sig ExGate extends FlowNode {
}

sig PaGate extends FlowNode {
}

sig IntermediateCatchEvent extends FlowNode {
}

sig IntermediateThrowEvent extends FlowNode {
}

sig TerminateEndEvent extends FlowNode {
}

sig EventBasedGateway extends FlowNode {
    outgoingEvents: set SequenceFlow
}

// Sequence flow
sig SequenceFlow {
	// Not needed atm.
	source: one FlowNode,
	target: one FlowNode
}

sig Event {
    trigger: set FlowNode
}

sig SubProcess extends FlowNode {
    subFlowNodes: set FlowNode,
    subStartEvent: one StartEvent,
    subEndEvents: set EndEvent  // Allows multiple end events
}

fact {
	// Make sequence flow source (target) symmetric to outgoingSequenceFlows (incomingSequenceFlows)
	all s:StartEvent {
		outSFs[s]
	}
	all a:Activity {
		incSFs[a]
		outSFs[a]
	}
	all e:EndEvent {
		incSFs[e]
	}
}

pred incSFs(f:FlowNode) {
		f.incomingSequenceFlows.target = f
}

pred outSFs(f:FlowNode) {
		f.outgoingSequenceFlows.source = f
}

//Token sig + handling
var sig Token {
	var pos: one StartEvent + Activity + SequenceFlow + IntermediateCatchEvent + IntermediateThrowEvent + EventBasedGateway
}

sig ProcessSnapshot {
	var tokens: set Token
}



pred startEventHappens(ps: ProcessSnapshot, s:StartEvent, t:Token) {
	-- pre conditions
	t in ps.tokens
	t.pos = s
	-- post condition
	some newToken : Token'-Token {
		Token' = Token + newToken - t
		//newToken.pos' = s.outgoingSequenceFlows
		pos' = pos + newToken->s.outgoingSequenceFlows - t->s
		tokens' = tokens  + ps->newToken - ps->t
	}
	-- frame conditions
}

pred activityStart(ps: ProcessSnapshot, a:Activity, sf:SequenceFlow) {
	-- pre conditions
	one t: ps.tokens {
		t.pos = sf and sf.target = a
		-- post conditions
		some newToken : Token'-Token {
			Token' = Token + newToken - t
			pos' = pos + newToken->a - t->sf
			tokens' = tokens  + ps->newToken - ps->t
		}
	}
	-- frame conditions
}

pred activityEnd(ps: ProcessSnapshot, a:Activity, sf:SequenceFlow) {
	one t: ps.tokens {
		t.pos = a
		some newToken : Token'-Token {
			Token' = Token + newToken - t
			pos' = pos + newToken->a.outgoingSequenceFlows - t->a
			tokens' = tokens  + ps->newToken - ps->t
		}
	}
}

pred exGateHappens (ps: ProcessSnapshot, x:ExGate, isf:SequenceFlow, osf:SequenceFlow) {
	-- pre conditions
	one t: ps.tokens {
		t.pos = isf and isf.target = x
		osf in x.outgoingSequenceFlows
		-- post conditions
		some newToken : Token'-Token {
			Token' = Token + newToken - t
			pos' = pos + newToken->osf - t->isf
			tokens' = tokens  + ps->newToken - ps->t
		}
	}
}

pred paGateHappens (ps: ProcessSnapshot, p:PaGate) {
	-- pre conditions
	let oldTokens = {t: Token | t.pos in p.incomingSequenceFlows} {
		#oldTokens.pos = #p.incomingSequenceFlows

		--post conditions
		let newTokens = Token' - Token {
			Token' = Token + newTokens - oldTokens
			(Token - oldTokens).pos' = (Token - oldTokens).pos
			tokens' = tokens  + ps->newTokens - ps->oldTokens
			newTokens.pos' = p.outgoingSequenceFlows
			#newTokens = #p.outgoingSequenceFlows
		}
	}
}

pred intermediateCatchEventStarts(ps: ProcessSnapshot, ice: IntermediateCatchEvent, sf: SequenceFlow) {
    -- pre conditions
    one t: ps.tokens {
        t.pos = sf and sf.target = ice
        -- post conditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ice - t->sf
            tokens' = tokens  + ps->newToken - ps->t
        }
    }
    -- frame conditions
}
pred intermediateCatchEventHappens(ps: ProcessSnapshot, ice: IntermediateCatchEvent, osf: SequenceFlow) {
    -- preconditions
    one t: ps.tokens {
        t.pos = ice
        osf in ice.outgoingSequenceFlows
        -- post conditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ice.outgoingSequenceFlows - t->ice
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred intermediateThrowEventStarts(ps: ProcessSnapshot, ite: IntermediateThrowEvent, sf: SequenceFlow) {
    -- pre conditions
    one t: ps.tokens {
        t.pos = sf and sf.target = ite
        -- post conditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ite - t->sf
            tokens' = tokens  + ps->newToken - ps->t
        }
    }
    -- frame conditions
}
pred intermediateThrowEventHappens(ps: ProcessSnapshot, ite: IntermediateThrowEvent, osf: SequenceFlow) {
    -- preconditions
    one t: ps.tokens {
        t.pos = ite
        osf in ite.outgoingSequenceFlows
        -- post conditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ite.outgoingSequenceFlows - t->ite
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred endEventHappens(ps: ProcessSnapshot, e:EndEvent, sf:SequenceFlow) {
	-- pre conditions
	one t: ps.tokens {
		sf in t.pos and sf.target = e
		-- post conditions
		Token' = Token - t
		pos' = pos - t->sf
		tokens' = tokens - ps->t
	}
	-- frame conditions
}

pred terminateEndEventHappens(ps: ProcessSnapshot, te:TerminateEndEvent, sf:SequenceFlow) {
	-- pre conditions
	one t: ps.tokens {
		sf in t.pos and sf.target = te
		-- post conditions
		-- remove all tokens as the process terminates immediately when this point is reached.
		ps.tokens' = ps.tokens - ps.tokens
	}
	-- frame conditions
}

pred eventBasedGatewayStarts(ps: ProcessSnapshot, ebg: EventBasedGateway, sf: SequenceFlow) {
    -- preconditions
    one t: ps.tokens {
        t.pos = sf and sf.target = ebg
        -- postconditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ebg - t->sf
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred eventBasedGatewayHappens(ps: ProcessSnapshot, ebg: EventBasedGateway, osf: SequenceFlow, ev: Event) {
    -- preconditions
    one t: ps.tokens {
        t.pos = ebg
        osf in ebg.outgoingEvents
        ev.trigger = osf.target
        -- postconditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->osf - t->ebg
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred enterSubProcess(ps: ProcessSnapshot, sp: SubProcess, sf: SequenceFlow) {
    -- Preconditions
    one t: ps.tokens {
        t.pos = sf and sf.target = sp
        -- Postconditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->sp.subStartEvent - t->sf
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred exitSubProcess(ps: ProcessSnapshot, sp: SubProcess, sf: SequenceFlow) {
    -- Preconditions
    one t: ps.tokens {
        t.pos = sp.subEndEvent
        sf in sp.outgoingSequenceFlows
        -- Postconditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->sf.target - t->sp.subEndEvent
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}


pred terminateCheck(ps: ProcessSnapshot) {
	#ps.tokens = 0
}

pred terminates[] {
	(some ps: ProcessSnapshot | terminateCheck[ps])
}

pred unsafe[] {
	(some t,t1 : Token | unsafeHelper[t,t1])
}

pred unsafeHelper(t,t1 : Token) {
	t != t1
	t.pos = t1.pos
}

pred init [] {
    #Process = 0
    #StartEvent = 1
    #EndEvent = 1
    #EventBasedGateway = 1
    #Event = 2
    #Token = 1
    #SequenceFlow = 3
    #ProcessSnapshot = 1
    
    some pSnapshot: ProcessSnapshot, s: StartEvent, ebg: EventBasedGateway, e: EndEvent, ev1, ev2: Event {
        #s.incomingSequenceFlows = 0
        #s.outgoingSequenceFlows = 1
        s.outgoingSequenceFlows.target = ebg
        
        #ebg.incomingSequenceFlows = 1
        ebg.incomingSequenceFlows.source = s
        #ebg.outgoingEvents = 2
        ebg.outgoingEvents.target = e

        // Define event triggers for the outgoing paths
        ev1.trigger = e  // Event triggers EndEvent directly
        ev2.trigger = e  // Event can also trigger another path, if applicable
        
        #e.incomingSequenceFlows = 1
        e.incomingSequenceFlows.source = ebg
        #e.outgoingSequenceFlows = 0

        // Place the initial token at the StartEvent
        one t: pSnapshot.tokens {
            t.pos = s
        }
    }
}

pred trans [] {
	(some ps: ProcessSnapshot, s: StartEvent, t:Token | startEventHappens[ps, s, t])
	or
	(some ps: ProcessSnapshot, a: Activity, sf:SequenceFlow | activityStart[ps, a, sf])
	or
	(some ps: ProcessSnapshot, a: Activity, sf:SequenceFlow | activityEnd[ps, a, sf])
	or
	(some ps: ProcessSnapshot, x: ExGate, isf,osf:SequenceFlow | exGateHappens[ps, x, isf, osf])
	or
	(some ps: ProcessSnapshot, p: PaGate | paGateHappens[ps, p])
	or
	(some ps: ProcessSnapshot, e: EndEvent, sf:SequenceFlow | endEventHappens[ps, e, sf])
    	or
    	(some ps: ProcessSnapshot, ice: IntermediateCatchEvent, sf: SequenceFlow | intermediateCatchEventStarts[ps, ice, sf])
    	or
    	(some ps: ProcessSnapshot, ice: IntermediateCatchEvent, osf: SequenceFlow | intermediateCatchEventHappens[ps, ice, osf])
    	or
    	(some ps: ProcessSnapshot, ite: IntermediateThrowEvent, sf: SequenceFlow | intermediateThrowEventStarts[ps, ite, sf])
    	or
    	(some ps: ProcessSnapshot, ite: IntermediateThrowEvent, osf: SequenceFlow | intermediateThrowEventHappens[ps, ite, osf])
    	or
	(some ps: ProcessSnapshot, te: TerminateEndEvent, sf: SequenceFlow | terminateEndEventHappens[ps, te, sf])
	or
    	(some ps: ProcessSnapshot, ebg: EventBasedGateway, sf: SequenceFlow | eventBasedGatewayStarts[ps, ebg, sf])
    	or
    	(some ps: ProcessSnapshot, ebg: EventBasedGateway, osf: SequenceFlow, ev: Event | eventBasedGatewayHappens[ps, ebg, osf, ev])
    	or
	doNothing
	// TODO: Expand with gateways
}

pred doNothing [] {
	-- the relevant relations stay the same
	tokens'=tokens
	Token'=Token
	pos'=pos
}

pred System {
	init and always trans-- and eventually terminates and not eventually unsafe
}

run System
