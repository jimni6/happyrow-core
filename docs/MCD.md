
```mermaid
	classDiagram  
    class User {
        +int id                   
        +string lastName
        +string firstName        
        +string email        
        +string password    
    }  
    class Event {        
	    +int id        
	    +string name        
	    +date date        
	    +string location        
	    +string type        
	    +int organizerId
    }  
    class Participant {
	    +int id        
	    +string status        
	    +int userId        
	    +int eventId    
	}  
    class Resource {        
	    +int id        
	    +string name        
	    +string category        
	    +int suggestedQuantity        
	    +int eventId    
	}  
    class Contribution {
        +int id
        +int participantId        
        +int resourceId        
        +int quantity    
    }  
    User "1" --> "0..*" Event : creates
    User "1" --> "0..*" Participant : is  
    Event "1" --> "0..*" Participant : hosts    
    Event "1" --> "0..*" Resource : needs  
    Participant "1" --> "0..*" Contribution : makes    
    Resource "1" --> "0..*" Contribution : receives
```

